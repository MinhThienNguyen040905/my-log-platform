package com.mylog.analysis.application;

import com.mylog.journal.application.JournalAnalysisAccess;
import com.mylog.safety.application.SafetyAnalysisPermission;
import com.mylog.user.application.UserProfileUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "mylog.jobs", name = "enabled", havingValue = "true")
public class AnalysisJobHandler {
    public enum Outcome { SUCCEEDED, STALE, POLICY_BLOCKED, CONSENT_REQUIRED }

    private final JournalAnalysisAccess journal;
    private final UserProfileUseCase users;
    private final SafetyAnalysisPermission safety;
    private final JournalAnalyzer analyzer;
    private final AnalysisStore store;
    private final TransactionTemplate tx;
    private final Clock clock;

    public AnalysisJobHandler(JournalAnalysisAccess journal, UserProfileUseCase users,
                              SafetyAnalysisPermission safety, JournalAnalyzer analyzer,
                              AnalysisStore store, TransactionTemplate tx, Clock clock) {
        this.journal = journal; this.users = users; this.safety = safety; this.analyzer = analyzer;
        this.store = store; this.tx = tx; this.clock = clock;
    }

    public Outcome process(UUID jobId, String workerId, UUID userId, UUID entryId, int contentVersion) {
        Preparation preparation = tx.execute(s -> prepare(userId, entryId, contentVersion));
        if (preparation.outcome != null) return preparation.outcome;
        long start = System.nanoTime();
        JournalAnalyzer.Result result = analyzer.analyze(preparation.input.title(), preparation.input.plainText());
        AnalysisOutputValidator.validate(result);
        long latency = Duration.ofNanos(System.nanoTime() - start).toMillis();
        return tx.execute(s -> finish(jobId, workerId, userId, entryId, contentVersion, result, latency));
    }

    private Preparation prepare(UUID userId, UUID entryId, int version) {
        if (!users.isConsentGranted(userId, "AI_PROCESSING")) {
            journal.cancel(userId, entryId, version, clock.instant());
            return new Preparation(null, Outcome.CONSENT_REQUIRED);
        }
        if (!safety.permits(userId, entryId)) {
            journal.block(userId, entryId, version, clock.instant());
            return new Preparation(null, Outcome.POLICY_BLOCKED);
        }
        JournalAnalysisAccess.Input input = journal.load(userId, entryId, version);
        if (input == null || !journal.start(userId, entryId, version, clock.instant()))
            return new Preparation(null, Outcome.STALE);
        return new Preparation(input, null);
    }

    private Outcome finish(UUID jobId, String workerId, UUID userId, UUID entryId, int version,
                           JournalAnalyzer.Result result, long latency) {
        if (!store.ownsLease(jobId, workerId, clock.instant())) return Outcome.STALE;
        boolean consent = users.isConsentGranted(userId, "AI_PROCESSING");
        boolean permitted = safety.permits(userId, entryId);
        if (!consent) {
            journal.cancel(userId, entryId, version, clock.instant());
            return Outcome.CONSENT_REQUIRED;
        }
        if (!permitted) {
            journal.block(userId, entryId, version, clock.instant());
            return Outcome.POLICY_BLOCKED;
        }
        boolean current = consent && permitted && journal.load(userId, entryId, version) != null;
        String policy = safety.policyVersion(userId, entryId);
        UUID analysisId = store.save(jobId, userId, entryId, version,
                policy == null ? "unknown" : policy, result, latency, current, clock.instant());
        if (!current) {
            if (!permitted) journal.block(userId, entryId, version, clock.instant());
            return Outcome.STALE;
        }
        if (journal.activate(userId, entryId, version, analysisId, clock.instant())) return Outcome.SUCCEEDED;
        store.markStale(analysisId);
        return Outcome.STALE;
    }

    public void terminalFailure(UUID userId, UUID entryId, int version) {
        tx.executeWithoutResult(s -> journal.fail(userId, entryId, version, clock.instant()));
    }

    private record Preparation(JournalAnalysisAccess.Input input, Outcome outcome) {}
}
