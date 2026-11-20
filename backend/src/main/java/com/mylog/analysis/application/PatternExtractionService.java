package com.mylog.analysis.application;

import com.mylog.journal.application.JournalAnalysisAccess;
import com.mylog.platform.web.DependencyUnavailableException;
import com.mylog.user.application.UserProfileUseCase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Reads the current user's last-30-day entries for thinking patterns. Only entries screened ALLOW are read,
 * consent (AI_PROCESSING and THINKING_PATTERNS) is checked before every provider call, and each entry version
 * is read once. Deliberately not transactional: the extractor may call a provider.
 */
@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class PatternExtractionService {
    public static final int WINDOW_DAYS = 30;
    static final int MAX_ENTRIES_PER_RUN = 40;

    /** {@code status}: OK or CONSENT_REQUIRED. {@code more}: entries are still waiting after this run. */
    public record Summary(String status, int read, int signals, int skipped, int failed, boolean more,
                          LocalDate from, LocalDate to) {}

    private final ObjectProvider<PatternExtractor> extractor;
    private final PatternSignalStore store;
    private final JournalAnalysisAccess journal;
    private final UserProfileUseCase users;
    private final Clock clock;

    public PatternExtractionService(ObjectProvider<PatternExtractor> extractor, PatternSignalStore store,
                                    JournalAnalysisAccess journal, UserProfileUseCase users, Clock clock) {
        this.extractor = extractor; this.store = store; this.journal = journal; this.users = users; this.clock = clock;
    }

    public Summary extractRecent(UUID userId) {
        PatternExtractor reader = extractor.getIfAvailable();
        if (reader == null) throw new DependencyUnavailableException();
        LocalDate to = LocalDate.now(clock.withZone(ZoneId.of(store.timezone(userId))));
        LocalDate from = to.minusDays(WINDOW_DAYS - 1L);
        if (!consented(userId)) return new Summary("CONSENT_REQUIRED", 0, 0, 0, 0, false, from, to);

        store.purgeStale(userId);
        List<PatternSignalStore.Candidate> pending = store.pending(userId, from, to, ThinkingPatternTaxonomy.VERSION,
                MAX_ENTRIES_PER_RUN + 1);
        int read = 0, signals = 0, skipped = 0, failed = 0;
        for (var candidate : pending.subList(0, Math.min(pending.size(), MAX_ENTRIES_PER_RUN))) {
            if (!consented(userId)) return new Summary("CONSENT_REQUIRED", read, signals, skipped, failed, true, from, to);
            var input = journal.loadForPatterns(userId, candidate.entryId(), candidate.contentVersion());
            if (input == null) { skipped++; continue; }
            long started = System.nanoTime();
            PatternExtractor.Result result = reader.extract(input.title(), input.plainText());
            store.recordUsage(result, Duration.ofNanos(System.nanoTime() - started).toMillis(), clock.instant());
            if (!result.succeeded()) { failed++; continue; }
            var accepted = PatternSignalValidator.accept(result.signals(), input.title(), input.plainText());
            if (store.save(userId, candidate, ThinkingPatternTaxonomy.VERSION, result, accepted, clock.instant())) {
                read++;
                signals += accepted.size();
            } else skipped++;
        }
        return new Summary("OK", read, signals, skipped, failed, pending.size() > MAX_ENTRIES_PER_RUN, from, to);
    }

    private boolean consented(UUID userId) {
        return users.isConsentGranted(userId, "AI_PROCESSING") && users.isConsentGranted(userId, "THINKING_PATTERNS");
    }
}
