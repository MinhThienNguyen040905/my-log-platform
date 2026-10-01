package com.mylog.user.application;

import com.mylog.identity.application.IdentityService;
import com.mylog.journal.application.JournalAssetDeletion;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.id.IdGenerator;
import com.mylog.platform.web.ConflictException;
import com.mylog.platform.web.ResourceNotFoundException;
import com.mylog.user.application.query.DeletionView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class AccountDeletionService {
    private final DeletionStore store;
    private final AccountPurge purge;
    private final JournalAssetDeletion assets;
    private final IdentityService identity;
    private final SensitiveDataCipher cipher;
    private final IdGenerator ids;
    private final Clock clock;
    private final TransactionTemplate transactions;
    private final boolean workerEnabled;
    @Value("${mylog.app-profile:all}") private String appProfile;
    public AccountDeletionService(DeletionStore store,AccountPurge purge,JournalAssetDeletion assets,
            IdentityService identity,SensitiveDataCipher cipher,IdGenerator ids,Clock clock,
            TransactionTemplate transactions,@Value("${mylog.deletion.enabled:true}") boolean workerEnabled) {
        this.store=store;this.purge=purge;this.assets=assets;this.identity=identity;
        this.cipher=cipher;this.ids=ids;this.clock=clock;this.transactions=transactions;
        this.workerEnabled=workerEnabled;
    }
    @Transactional
    public DeletionView request(UUID userId,String password) {
        identity.reauthenticate(userId,password);
        Instant now=clock.instant();
        try {
            DeletionView view=store.create(ids.next(),userId,cipher.lookupHash("deleted-user:"+userId),
                    now,now.plus(7,ChronoUnit.DAYS));
            identity.beginDeletion(userId);
            return view;
        } catch (DataIntegrityViolationException e) { throw new ConflictException("Deletion already requested."); }
    }
    @Transactional(readOnly = true)
    public DeletionView get(UUID userId,UUID id) {
        return store.find(userId,id).orElseThrow(() -> new ResourceNotFoundException("Deletion request not found."));
    }
    public DeletionView status(UUID id,String email,String password,String remoteAddress) {
        UUID userId=identity.authenticateForCancellation(email,password,remoteAddress);
        return transactions.execute(s -> store.find(userId,id)
                .orElseThrow(() -> new ResourceNotFoundException("Deletion request not found.")));
    }
    public void cancel(UUID id,String email,String password,String remoteAddress) {
        UUID userId=identity.authenticateForCancellation(email,password,remoteAddress);
        transactions.executeWithoutResult(s -> {
            if (!store.cancel(userId,id,clock.instant()))
                throw new ResourceNotFoundException("Deletion request not found.");
            identity.cancelDeletion(userId);
        });
    }
    @Scheduled(fixedDelayString = "${mylog.deletion.poll-delay-ms:5000}")
    public void poll() {
        if (!workerEnabled || "api".equals(appProfile)) return;
        transactions.executeWithoutResult(s -> store.purgeAudit(clock.instant().minus(365,ChronoUnit.DAYS)));
        DeletionStore.Work work=transactions.execute(s -> store.claim(clock.instant()).orElse(null));
        if (work==null) return;
        try {
            if (!"ASSETS_DELETED".equals(work.checkpoint())) {
                assets.deleteAll(work.userId());
                transactions.executeWithoutResult(s -> store.checkpoint(work.id(),"ASSETS_DELETED"));
            }
            transactions.executeWithoutResult(s -> {
                purge.purge(work.userId());
                store.complete(work.id(),clock.instant());
            });
        } catch (Exception e) {
            transactions.executeWithoutResult(s -> store.fail(work.id(),clock.instant().plusSeconds(
                    Math.min(86400,60L << Math.min(work.attempt(),10)))));
        }
    }
}
