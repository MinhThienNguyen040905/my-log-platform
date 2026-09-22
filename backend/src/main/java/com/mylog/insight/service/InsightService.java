package com.mylog.insight.service;

import com.mylog.common.api.ApiErrorCodes;
import com.mylog.common.exception.ResourceNotFoundException;
import com.mylog.insight.config.InsightProperties;
import com.mylog.insight.repository.InsightCandidateRepository;
import com.mylog.insight.repository.InsightCandidateRepository.TopicMoodCandidate;
import com.mylog.insight.repository.InsightReadRepository;
import com.mylog.insight.repository.InsightReadRepository.InsightRow;
import com.mylog.insight.repository.InsightRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InsightService {

    private static final BigDecimal MINIMUM_MOOD_DELTA = new BigDecimal("0.50");

    private final InsightRepository repository;
    private final InsightCandidateRepository candidates;
    private final InsightReadRepository reads;
    private final InsightConfidencePolicy confidencePolicy;
    private final InsightProperties properties;
    private final Clock clock;

    public InsightService(
            InsightRepository repository, InsightCandidateRepository candidates, InsightReadRepository reads,
            InsightConfidencePolicy confidencePolicy,
            InsightProperties properties, Clock clock) {
        this.repository = repository;
        this.candidates = candidates;
        this.reads = reads;
        this.confidencePolicy = confidencePolicy;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public void refresh(UUID userId) {
        var locale = candidates.findUserLocale(userId);
        ZoneId zone = validZone(locale.timezone());
        LocalDate to = LocalDate.now(clock.withZone(zone));
        LocalDate from = to.minusDays(properties.lookbackDays() - 1L);
        Instant now = clock.instant();
        repository.advanceLifecycle(userId, now);
        for (TopicMoodCandidate candidate : candidates.topicMoodCandidates(userId, from, to, zone.getId())) {
            createOrUpdate(userId, candidate, from, to, now, locale.language());
        }
    }

    @Transactional(readOnly = true)
    public List<InsightView> list(UUID userId) {
        return reads.findVisible(userId).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public InsightView get(UUID userId, UUID insightId) {
        return reads.findOwned(userId, insightId)
                .map(this::view)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ApiErrorCodes.INSIGHT_NOT_FOUND, "Insight does not exist"));
    }

    private void createOrUpdate(
            UUID userId, TopicMoodCandidate candidate, LocalDate from, LocalDate to,
            Instant now, String language) {
        BigDecimal delta = candidate.moodDelta();
        if (delta.abs().compareTo(MINIMUM_MOOD_DELTA) < 0) return;
        String confidence = confidencePolicy.classify(candidate.distinctDays(), candidate.matchingRatio());
        if (confidence == null) return;
        String direction = delta.signum() >= 0 ? "HIGHER" : "LOWER";
        String fingerprint = "topic_mood_v1:" + candidate.normalizedName() + ":" + direction.toLowerCase();
        String title = title(candidate.displayName(), direction, language);
        String description = description(candidate.displayName(), direction, language);
        UUID insightId = repository.upsertInsight(
                userId, fingerprint, title, description, confidence, from, to, now);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("topic", candidate.displayName());
        details.put("distinctDays", candidate.distinctDays());
        details.put("topicMoodAverage", candidate.topicMood());
        details.put("overallMoodAverage", candidate.overallMood());
        details.put("matchingRatio", candidate.matchingRatio());
        details.put("direction", direction);
        details.put("interpretation", "ASSOCIATION_NOT_CAUSATION");
        repository.replaceEvidence(
                insightId, candidate.journalCount(), candidate.matchingCount(), delta, details, now);

        if (!candidate.safetyBlocked()) {
            repository.ensureSuggestedAction(
                    userId, insightId, suggestedAction(candidate.displayName(), language), now);
        }
    }

    private InsightView view(InsightRow row) {
        return new InsightView(
                row.id(), row.type(), row.title(), row.description(), row.confidence(), row.status(),
                row.periodStart(), row.periodEnd(), row.createdAt(), row.updatedAt(),
                reads.findEvidence(row.id()).stream().map(item -> new InsightView.Evidence(
                        item.id(), item.type(), item.sampleSize(), item.matchingCount(), item.metric(),
                        item.numericValue(), item.unit(), reads.parseDetails(item.detailsJson()),
                        item.calculationVersion())).toList(),
                reads.findActions(row.id()).stream().map(item -> new InsightView.Action(
                        item.id(), item.description(), item.status(), item.createdAt(), item.respondedAt())).toList());
    }

    private ZoneId validZone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            return ZoneId.of("UTC");
        }
    }

    private String title(String topic, String direction, String language) {
        if ("en".equals(language)) {
            return "Mood was " + direction.toLowerCase() + " on days mentioning “" + topic + "”";
        }
        return "Tâm trạng " + ("HIGHER".equals(direction) ? "cao hơn" : "thấp hơn")
                + " trong những ngày có chủ đề “" + topic + "”";
    }

    private String description(String topic, String direction, String language) {
        if ("en".equals(language)) {
            return "In this period, entries mentioning “" + topic + "” coincided with "
                    + direction.toLowerCase() + " mood scores. This is an observed association, not causation.";
        }
        return "Trong giai đoạn này, các bài viết nhắc đến “" + topic + "” đi cùng điểm tâm trạng "
                + ("HIGHER".equals(direction) ? "cao hơn" : "thấp hơn")
                + ". Đây là mối liên hệ quan sát được, không phải quan hệ nguyên nhân.";
    }

    private String suggestedAction(String topic, String language) {
        if ("en".equals(language)) {
            return "Next time “" + topic + "” comes up, note one small contextual factor that may matter.";
        }
        return "Lần tới khi viết về “" + topic + "”, hãy ghi thêm một yếu tố bối cảnh nhỏ mà bạn thấy đáng chú ý.";
    }
}
