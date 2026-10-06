package com.mylog.analysis.infrastructure.persistence;

import com.mylog.analysis.infrastructure.persistence.entity.AiAnalysis;
import com.mylog.analysis.infrastructure.persistence.entity.AiUsageRecord;
import com.mylog.analysis.infrastructure.persistence.entity.AnalysisEmotion;
import com.mylog.analysis.infrastructure.persistence.entity.AnalysisTopic;

import com.mylog.analysis.application.AnalysisStore;
import com.mylog.analysis.application.JournalAnalyzer;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.id.IdGenerator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Repository
@ConditionalOnProperty(prefix = "mylog.jobs", name = "enabled", havingValue = "true")
class JpaAnalysisStore implements AnalysisStore {
    private final EntityManager em;
    private final SensitiveDataCipher cipher;
    private final IdGenerator ids;
    private final ObjectMapper mapper;
    private final MeterRegistry metrics;

    JpaAnalysisStore(EntityManager em, SensitiveDataCipher cipher, IdGenerator ids,
                     ObjectMapper mapper, MeterRegistry metrics) {
        this.em = em; this.cipher = cipher; this.ids = ids; this.mapper = mapper; this.metrics = metrics;
    }

    @Override public boolean ownsLease(UUID jobId, String workerId, Instant now) {
        @SuppressWarnings("unchecked")
        var rows = em.createNativeQuery("""
                SELECT locked_by FROM ai_jobs WHERE id=?1 AND status='PROCESSING'
                    AND lease_expires_at>?2 FOR UPDATE
                """, String.class).setParameter(1, jobId).setParameter(2, now).getResultList();
        return !rows.isEmpty() && workerId.equals(rows.getFirst());
    }

    @Override public UUID save(UUID jobId, UUID userId, UUID entryId, int contentVersion,
                               String safetyPolicyVersion, JournalAnalyzer.Result result,
                               long latencyMs, boolean current, Instant now) {
        AiAnalysis analysis = new AiAnalysis();
        analysis.id = ids.next();
        analysis.journalEntryId = entryId;
        analysis.userId = userId;
        analysis.contentVersion = contentVersion;
        Object version = em.createNativeQuery("""
                SELECT coalesce(max(analysis_version),0)+1 FROM ai_analyses
                WHERE journal_entry_id=?1 AND content_version=?2
                """).setParameter(1, entryId).setParameter(2, contentVersion).getSingleResult();
        analysis.analysisVersion = ((Number) version).intValue();
        analysis.status = current ? "SUCCEEDED" : "STALE";
        analysis.sentimentLabel = result.sentiment();
        analysis.sentimentScore = result.sentimentScore();
        var encrypted = cipher.encrypt("ai_analyses.output", userId, analysis.id,
                mapper.writeValueAsString(Map.of("reflection", result.reflection())));
        analysis.encryptedOutput = encrypted.ciphertext();
        analysis.outputIv = encrypted.iv();
        analysis.wrappedDataKey = encrypted.wrappedKey();
        analysis.encryptionKeyVersion = encrypted.keyVersion();
        analysis.outputSchemaVersion = 1;
        analysis.provider = result.provider();
        analysis.model = result.model();
        analysis.modelVersion = result.modelVersion();
        analysis.promptTemplateVersion = result.promptVersion();
        analysis.safetyPolicyVersion = safetyPolicyVersion;
        analysis.startedAt = now.minusMillis(Math.max(0, latencyMs));
        analysis.completedAt = now;
        analysis.createdAt = now;
        em.persist(analysis);
        short rank = 1;
        for (JournalAnalyzer.ScoredCode item : result.emotions()) {
            AnalysisEmotion emotion = new AnalysisEmotion();
            emotion.analysisId = analysis.id;
            emotion.emotionCode = item.code();
            emotion.score = item.score();
            emotion.rank = rank++;
            em.persist(emotion);
        }
        rank = 1;
        for (JournalAnalyzer.ScoredCode item : result.topics()) {
            AnalysisTopic topic = new AnalysisTopic();
            topic.id = ids.next();
            topic.analysisId = analysis.id;
            topic.topicCode = item.code();
            topic.score = item.score();
            topic.rank = rank++;
            em.persist(topic);
        }
        AiUsageRecord usage = new AiUsageRecord();
        usage.id = ids.next();
        usage.jobId = jobId;
        usage.analysisId = analysis.id;
        usage.provider = result.provider();
        usage.model = result.model();
        usage.operation = "JOURNAL_ANALYSIS";
        usage.inputTokens = result.inputTokens();
        usage.outputTokens = result.outputTokens();
        usage.estimatedCostUsd = result.estimatedCostUsd();
        usage.latencyMs = Math.toIntExact(Math.min(Integer.MAX_VALUE, latencyMs));
        usage.requestStatus = "SUCCEEDED";
        usage.createdAt = now;
        em.persist(usage);
        em.flush();
        metrics.counter("mylog.ai.tokens", "provider", result.provider(), "kind", "input")
                .increment(result.inputTokens());
        metrics.counter("mylog.ai.tokens", "provider", result.provider(), "kind", "output")
                .increment(result.outputTokens());
        DistributionSummary.builder("mylog.ai.estimated.cost.usd")
                .tag("provider", result.provider()).register(metrics)
                .record(result.estimatedCostUsd().doubleValue());
        Timer.builder("mylog.ai.provider.latency").tag("provider", result.provider())
                .register(metrics).record(latencyMs, TimeUnit.MILLISECONDS);
        return analysis.id;
    }

    @Override public void markStale(UUID analysisId) {
        em.createQuery("update AiAnalysis a set a.status='STALE' where a.id=:id")
                .setParameter("id", analysisId).executeUpdate();
    }
}
