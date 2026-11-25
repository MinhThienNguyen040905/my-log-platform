package com.mylog.user.application;

import com.mylog.identity.application.IdentityService;
import com.mylog.identity.application.VerificationDelivery;
import com.mylog.journal.application.JournalAssetService;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.id.IdGenerator;
import com.mylog.platform.web.ConflictException;
import com.mylog.user.application.query.DeletionView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Account deletion (UC-23). Off unless {@code mylog.account-deletion.enabled=true}: then neither the endpoint nor
 * the worker exists. Cancelling by signing in again is handled by {@link DeletionCancellationOnSignIn}.
 */
@Service
@ConditionalOnProperty(prefix = "mylog", name = {"identity.enabled", "account-deletion.enabled"}, havingValue = "true")
public class AccountDeletionService {
    static final int STUCK_AFTER = 10;
    private final DeletionStore store;
    private final AccountPurge purge;
    private final JournalAssetService assets;
    private final IdentityService identity;
    private final VerificationDelivery mail;
    private final SensitiveDataCipher cipher;
    private final IdGenerator ids;
    private final Clock clock;
    private final TransactionTemplate transactions;
    @Value("${mylog.app-profile:all}") private String appProfile;
    public AccountDeletionService(DeletionStore store,AccountPurge purge,JournalAssetService assets,
            IdentityService identity,VerificationDelivery mail,SensitiveDataCipher cipher,IdGenerator ids,Clock clock,
            TransactionTemplate transactions) {
        this.store=store;this.purge=purge;this.assets=assets;this.identity=identity;this.mail=mail;
        this.cipher=cipher;this.ids=ids;this.clock=clock;this.transactions=transactions;
    }

    /** Confirms identity, schedules deletion in 7 days, signs the user out everywhere, then emails the date. */
    public DeletionView request(UUID userId,UUID sessionId,String password) {
        identity.confirmIdentity(userId,sessionId,password,"account-deletion");
        Instant now=clock.instant();
        DeletionView view;
        try {
            view=transactions.execute(s -> {
                DeletionView created=store.create(ids.next(),userId,cipher.lookupHash("deleted-user:"+userId),
                        now,now.plus(7,ChronoUnit.DAYS));
                identity.beginDeletion(userId);
                return created;
            });
        } catch (DataIntegrityViolationException e) { throw new ConflictException("Deletion already requested."); }
        mail.sendAccountDeletionScheduled(identity.exportEmail(userId),view.scheduledFor());
        return view;
    }

    @Scheduled(fixedDelayString = "${mylog.account-deletion.poll-delay-ms:5000}")
    public void poll() {
        if ("api".equals(appProfile)) return;
        transactions.executeWithoutResult(s -> store.purgeAudit(clock.instant().minus(365,ChronoUnit.DAYS)));
        DeletionStore.Work work=transactions.execute(s -> store.claim(clock.instant()).orElse(null));
        if (work==null) return;
        String email;
        try {
            // One transaction: files are queued for storage deletion together with the purge of their rows.
            email=transactions.execute(s -> {
                String address=identity.exportEmail(work.userId());
                assets.enqueueAllForDeletion(work.userId());
                purge.purge(work.userId());
                store.complete(work.id(),clock.instant());
                return address;
            });
        } catch (Exception e) {
            String code=work.attempt()>=STUCK_AFTER ? "DELETION_STUCK" : "CLEANUP_RETRY";
            transactions.executeWithoutResult(s -> store.fail(work.id(),clock.instant().plusSeconds(
                    Math.min(86400,60L << Math.min(work.attempt(),10))),code));
            return;
        }
        mail.sendAccountDeleted(email);
    }
}
