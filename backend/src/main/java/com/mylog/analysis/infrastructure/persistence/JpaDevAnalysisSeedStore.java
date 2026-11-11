package com.mylog.analysis.infrastructure.persistence;

import com.mylog.analysis.application.DevAnalysisSeedStore;
import com.mylog.analysis.application.JournalAnalyzer;
import com.mylog.analysis.infrastructure.persistence.entity.AiAnalysis;
import com.mylog.analysis.infrastructure.persistence.entity.AnalysisEmotion;
import com.mylog.analysis.infrastructure.persistence.entity.AnalysisTopic;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.id.IdGenerator;
import com.mylog.platform.web.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@Profile("local & !staging & !prod")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaDevAnalysisSeedStore implements DevAnalysisSeedStore {
    private final EntityManager em;
    private final SensitiveDataCipher cipher;
    private final IdGenerator ids;
    private final ObjectMapper mapper;

    JpaDevAnalysisSeedStore(EntityManager em, SensitiveDataCipher cipher, IdGenerator ids, ObjectMapper mapper) {
        this.em = em; this.cipher = cipher; this.ids = ids; this.mapper = mapper;
    }

    @Override @Transactional(readOnly = true)
    public String timezone(UUID userId) {
        @SuppressWarnings("unchecked")
        List<String> values = em.createNativeQuery("SELECT timezone FROM user_profiles WHERE user_id=?1", String.class)
                .setParameter(1, userId).getResultList();
        if (values.isEmpty()) throw new ResourceNotFoundException("Không tìm thấy hồ sơ.");
        return values.getFirst();
    }

    @Override @Transactional(readOnly = true)
    public List<Candidate> candidates(UUID userId, LocalDate from, LocalDate to) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT j.id, j.content_version, j.local_date, j.mood_code, j.mood_score, j.stress_score,
                    j.sleep_minutes, string_agg(t.tag_id::text, ',')
                FROM journal_entries j
                LEFT JOIN journal_entry_tags t ON t.journal_entry_id=j.id
                WHERE j.user_id=?1 AND j.local_date BETWEEN ?2 AND ?3
                    AND j.deleted_at IS NULL AND j.entry_status='SAVED'
                    AND j.analysis_status IN ('NOT_REQUESTED','PENDING','ANALYSIS_OUTDATED','ANALYSIS_FAILED')
                GROUP BY j.id
                ORDER BY j.local_date, j.occurred_at
                """).setParameter(1, userId).setParameter(2, from).setParameter(3, to).getResultList();
        List<Candidate> out = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            List<UUID> tagIds = row[7] == null ? List.of()
                    : Arrays.stream(((String) row[7]).split(",")).map(UUID::fromString).toList();
            out.add(new Candidate((UUID) row[0], ((Number) row[1]).intValue(), (LocalDate) row[2],
                    (String) row[3], (BigDecimal) row[4], (BigDecimal) row[5],
                    row[6] == null ? null : ((Number) row[6]).intValue(), tagIds));
        }
        return out;
    }

    @Override @Transactional
    public UUID save(UUID userId, UUID entryId, int contentVersion, JournalAnalyzer.Result result, Instant now) {
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
        analysis.status = "SUCCEEDED";
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
        analysis.safetyPolicyVersion = "LOCAL_DEV_SEED";
        analysis.startedAt = now;
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
        em.flush();
        return analysis.id;
    }
}
