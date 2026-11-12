package com.mylog.analysis.application;

import com.mylog.journal.application.JournalAnalysisAccess;
import com.mylog.journal.application.JournalTagService;
import com.mylog.journal.application.query.JournalTagView;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Option 1a: marks the current user's last-30-day entries as analyzed with {@link SyntheticAnalysis}.
 * Exists only in the {@code local} profile; never sends journal content anywhere and never reads it.
 */
@Service
@Profile("local & !staging & !prod")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class DevAnalysisSeedService {
    public static final int WINDOW_DAYS = 30;
    private static final String SAFETY_POLICY = "LOCAL_DEV_SEED";

    private final DevAnalysisSeedStore store;
    private final JournalAnalysisAccess journal;
    private final JournalTagService tags;
    private final Clock clock;

    public DevAnalysisSeedService(DevAnalysisSeedStore store, JournalAnalysisAccess journal,
                                  JournalTagService tags, Clock clock) {
        this.store = store; this.journal = journal; this.tags = tags; this.clock = clock;
    }

    public record Result(int analyzed, int skipped, LocalDate from, LocalDate to) {}

    public Result seedRecent(UUID userId) {
        return seedRecent(userId, false);
    }

    /** {@code reseed} first replaces earlier synthetic analyses of the window (never real ones). */
    public Result seedRecent(UUID userId, boolean reseed) {
        LocalDate to = LocalDate.now(clock.withZone(ZoneId.of(store.timezone(userId))));
        LocalDate from = to.minusDays(WINDOW_DAYS - 1L);
        if (reseed) store.resetSynthetic(userId, from, to);
        Map<UUID, String> topicByTag = new HashMap<>();
        for (JournalTagView tag : tags.list(userId)) {
            String topic = topicForTag(tag.name());
            if (topic != null) topicByTag.put(tag.id(), topic);
        }
        int analyzed = 0, skipped = 0;
        for (var candidate : store.candidates(userId, from, to)) {
            Set<String> hints = new LinkedHashSet<>();
            for (UUID tagId : candidate.tagIds()) if (topicByTag.containsKey(tagId)) hints.add(topicByTag.get(tagId));
            var result = SyntheticAnalysis.generate(new SyntheticAnalysis.Input(candidate.entryId(),
                    candidate.moodCode(), candidate.moodScore(), candidate.stressScore(),
                    candidate.sleepMinutes(), hints));
            AnalysisOutputValidator.validate(result);
            var now = clock.instant();
            if (!journal.start(userId, candidate.entryId(), candidate.contentVersion(), now)) { skipped++; continue; }
            try {
                UUID analysisId = store.save(userId, candidate.entryId(), candidate.contentVersion(), result, now);
                if (journal.activate(userId, candidate.entryId(), candidate.contentVersion(), analysisId, clock.instant())) analyzed++;
                else skipped++;
            } catch (RuntimeException e) {
                journal.fail(userId, candidate.entryId(), candidate.contentVersion(), clock.instant());
                throw e;
            }
        }
        return new Result(analyzed, skipped, from, to);
    }

    /** Maps the default Vietnamese tag names used by the seed script onto the approved topic codes. */
    static String topicForTag(String name) {
        if (name == null) return null;
        return switch (name.trim().toLowerCase(Locale.ROOT)) {
            case "công việc", "học tập" -> "WORK";
            case "gia đình", "bạn bè" -> "RELATIONSHIPS";
            case "sức khỏe" -> "HEALTH";
            case "nghỉ ngơi" -> "SELF_CARE";
            default -> null;
        };
    }
}
