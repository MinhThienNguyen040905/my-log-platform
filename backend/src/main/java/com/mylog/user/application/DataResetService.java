package com.mylog.user.application;

import com.mylog.identity.application.IdentityService;
import com.mylog.journal.application.JournalAssetService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.UUID;

/**
 * "Start over" (UC-29): permanently deletes the user's journal, check-ins, analyses, insights, reports, self-care
 * data, exports, feedback and safety events, and returns the profile to defaults, keeping the account, consents
 * and sessions. Identity is confirmed first ({@link IdentityService#confirmIdentity}). Stored image files are
 * queued for deletion in the same transaction, so a storage outage never loses track of them.
 */
@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class DataResetService {
    private final IdentityService identity;
    private final AccountDataReset reset;
    private final JournalAssetService assets;
    private final UserProfileUseCase profiles;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public DataResetService(IdentityService identity, AccountDataReset reset, JournalAssetService assets,
                            UserProfileUseCase profiles, TransactionTemplate transactions, Clock clock) {
        this.identity = identity; this.reset = reset; this.assets = assets; this.profiles = profiles;
        this.transactions = transactions; this.clock = clock;
    }

    public void reset(UUID userId, UUID sessionId, String password) {
        identity.confirmIdentity(userId, sessionId, password, "data-reset");
        transactions.executeWithoutResult(status -> {
            reset.lock(userId);
            assets.enqueueAllForDeletion(userId);
            reset.reset(userId, clock.instant());
            profiles.resetPersonalDetails(userId);
        });
    }
}
