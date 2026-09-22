package com.mylog.analysis.service;

import com.mylog.analysis.service.CorrectionCommand.EmotionCorrection;
import com.mylog.analysis.service.CorrectionCommand.Operation;
import com.mylog.analysis.service.CorrectionCommand.TopicCorrection;
import com.mylog.analysis.repository.CorrectionRepository;
import com.mylog.analysis.service.AnalysisQueryService.JournalState;
import com.mylog.common.api.ApiErrorCodes;
import com.mylog.common.exception.BadRequestException;
import com.mylog.common.exception.ConflictException;
import com.mylog.common.messaging.MessagingTopology;
import com.mylog.common.outbox.OutboxWriter;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CorrectionService {

    private static final Set<String> EMOTIONS = Set.of(
            "JOY", "SADNESS", "ANGER", "FEAR", "ANXIETY", "CALM", "HOPE",
            "GRATITUDE", "LONELINESS", "FRUSTRATION", "EXCITEMENT");

    private final CorrectionRepository repository;
    private final AnalysisQueryService query;
    private final OutboxWriter outbox;
    private final Clock clock;

    public CorrectionService(
            CorrectionRepository repository, AnalysisQueryService query, OutboxWriter outbox, Clock clock) {
        this.repository = repository;
        this.query = query;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional
    public void correct(UUID userId, UUID journalId, CorrectionCommand request) {
        JournalState journal = query.ownedJournal(userId, journalId);
        UUID analysisId = currentAnalysis(journalId, journal.version());
        List<EmotionCorrection> emotions = request.emotions() == null ? List.of() : request.emotions();
        List<TopicCorrection> topics = request.topics() == null ? List.of() : request.topics();
        if (emotions.isEmpty() && topics.isEmpty()) {
            throw new BadRequestException(ApiErrorCodes.INVALID_REQUEST, "At least one correction is required");
        }
        Instant now = clock.instant();
        emotions.forEach(item -> correctEmotion(userId, journalId, analysisId, item, now));
        topics.forEach(item -> correctTopic(userId, journalId, analysisId, item, now));
        outbox.append("JOURNAL", journalId, MessagingTopology.JOURNAL_CORRECTED, 1,
                Map.of("journalId", journalId.toString(), "userId", userId.toString(), "journalVersion", journal.version()));
    }

    private void correctEmotion(
            UUID userId, UUID journalId, UUID analysisId, EmotionCorrection item, Instant now) {
        String type = item.type().trim().toUpperCase(Locale.ROOT);
        if (!EMOTIONS.contains(type) || (item.operation() != Operation.REMOVE && item.score() == null)) {
            throw new BadRequestException(ApiErrorCodes.INVALID_REQUEST, "Emotion correction is invalid");
        }
        BigDecimal original = repository.findEmotionOriginalScore(analysisId, type).orElse(null);
        BigDecimal effective = item.operation() == Operation.REMOVE ? BigDecimal.ZERO : item.score();
        if (original == null) {
            repository.insertEmotion(analysisId, type, effective, now);
        } else {
            repository.updateEmotion(analysisId, type, effective, now);
        }
        audit(userId, journalId, analysisId, "EMOTION", type, item.operation(),
                original == null ? null : Map.of("score", original), Map.of("score", effective), now);
    }

    private void correctTopic(
            UUID userId, UUID journalId, UUID analysisId, TopicCorrection item, Instant now) {
        String displayName = item.name().trim();
        String normalized = normalizeTopic(displayName);
        UUID topicId = repository.upsertTopic(normalized, displayName, now);
        Map<String, Object> original = repository.findJournalTopic(journalId, topicId).orElse(null);
        if (item.operation() == Operation.REMOVE) {
            if (original != null) {
                repository.deactivateTopic(journalId, topicId, now);
            }
        } else {
            BigDecimal confidence = item.confidence() == null ? BigDecimal.ONE : item.confidence();
            repository.upsertUserTopic(journalId, topicId, confidence, now);
        }
        Map<String, Object> corrected = new LinkedHashMap<>();
        corrected.put("active", item.operation() != Operation.REMOVE);
        corrected.put("confidence", item.confidence());
        audit(userId, journalId, analysisId, "TOPIC", normalized, item.operation(), original, corrected, now);
    }

    private void audit(
            UUID userId, UUID journalId, UUID analysisId, String fieldType, String fieldKey,
            Operation operation, Object original, Object corrected, Instant now) {
        repository.insertAudit(
                userId, journalId, analysisId, fieldType, fieldKey, operation.name(), original, corrected, now);
    }

    private UUID currentAnalysis(UUID journalId, long version) {
        return repository.findCurrentAnalysisId(journalId, version)
                .orElseThrow(() -> new ConflictException(
                        ApiErrorCodes.ANALYSIS_NOT_READY, "Current analysis is not completed"));
    }

    private String normalizeTopic(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
