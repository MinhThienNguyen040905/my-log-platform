package com.mylog.user.application;

import com.mylog.identity.application.IdentityService;
import com.mylog.identity.application.query.SessionSummary;
import com.mylog.journal.application.JournalAssetService;
import com.mylog.journal.application.JournalAssetService.StoredFile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * "Start a new journey": permanently deletes the user's journal, check-ins, analyses, insights, reports,
 * self-care data, exports, feedback and safety events, keeping the account. Accounts with a password confirm
 * it (rate limited); passwordless (Google) accounts must have signed in within the last 10 minutes.
 * Stored image files are removed after the database commit.
 */
@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class DataResetService {
    static final Duration RECENT_LOGIN = Duration.ofMinutes(10);
    private final IdentityService identity;
    private final AccountDataReset reset;
    private final JournalAssetService assets;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public DataResetService(IdentityService identity, AccountDataReset reset, JournalAssetService assets,
                            TransactionTemplate transactions, Clock clock) {
        this.identity = identity; this.reset = reset; this.assets = assets; this.transactions = transactions; this.clock = clock;
    }

    public void reset(UUID userId, UUID sessionId, String password) {
        confirmIdentity(userId, sessionId, password);
        List<StoredFile> files = transactions.execute(status -> {
            reset.lock(userId);
            List<StoredFile> stored = assets.storedFiles(userId);
            reset.reset(userId, clock.instant());
            return stored;
        });
        assets.deleteStoredFiles(Objects.requireNonNullElse(files, List.of()));
    }

    private void confirmIdentity(UUID userId, UUID sessionId, String password) {
        if (identity.hasPassword(userId)) {
            try { identity.reauthenticateLimited(userId, password, "data-reset"); }
            catch (IdentityService.InvalidCredentialsException e) { throw new DataResetDeniedException("PASSWORD_INCORRECT"); }
            return;
        }
        Instant signedIn = identity.sessions(userId).stream()
                .filter(session -> session.id().equals(sessionId))
                .map(SessionSummary::createdAt).findFirst().orElse(null);
        if (signedIn == null || signedIn.isBefore(clock.instant().minus(RECENT_LOGIN)))
            throw new DataResetDeniedException("RECENT_LOGIN_REQUIRED");
    }
}
