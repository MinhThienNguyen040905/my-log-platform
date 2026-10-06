package com.mylog.analysis.infrastructure.persistence;

import com.mylog.analysis.infrastructure.persistence.entity.AiAnalysis;
import com.mylog.analysis.infrastructure.persistence.entity.AnalysisEmotion;
import com.mylog.analysis.infrastructure.persistence.entity.AnalysisTopic;

import com.mylog.analysis.application.AnalysisQueries;
import com.mylog.analysis.application.query.AnalysisView;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.web.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaAnalysisQueries implements AnalysisQueries {
    private final EntityManager em;
    private final SensitiveDataCipher cipher;
    private final ObjectMapper mapper;

    JpaAnalysisQueries(EntityManager em, SensitiveDataCipher cipher, ObjectMapper mapper) {
        this.em = em; this.cipher = cipher; this.mapper = mapper;
    }

    @Override @Transactional(readOnly = true)
    public AnalysisView get(UUID userId, UUID entryId) {
        return load(userId,entryId,false);
    }

    @Override
    public AnalysisView exportRetained(UUID userId, UUID entryId) {
        return load(userId,entryId,true);
    }

    private AnalysisView load(UUID userId,UUID entryId,boolean includeDeleted) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT content_version, analysis_status, latest_analysis_id FROM journal_entries
                WHERE id=?1 AND user_id=?2 AND (?3 OR deleted_at IS NULL)
                """).setParameter(1, entryId).setParameter(2, userId)
                .setParameter(3,includeDeleted).getResultList();
        if (rows.isEmpty()) throw new ResourceNotFoundException("Không tìm thấy nhật ký.");
        Object[] row = rows.getFirst();
        int version = ((Number) row[0]).intValue();
        String status = row[1].toString();
        UUID analysisId = (UUID) row[2];
        if (!"ANALYZED".equals(status) || analysisId == null)
            return new AnalysisView(entryId, version, status, null, null, null, List.of(), List.of(), null);
        AiAnalysis analysis = em.createQuery("""
                select a from AiAnalysis a where a.id=:id and a.userId=:userId
                    and a.journalEntryId=:entryId and a.contentVersion=:version and a.status='SUCCEEDED'
                """, AiAnalysis.class).setParameter("id", analysisId).setParameter("userId", userId)
                .setParameter("entryId", entryId).setParameter("version", version)
                .getResultStream().findFirst().orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân tích."));
        String json = cipher.decrypt("ai_analyses.output", userId, analysisId,
                new SensitiveDataCipher.Encrypted(analysis.encryptedOutput, analysis.outputIv,
                        analysis.wrappedDataKey, analysis.encryptionKeyVersion));
        String reflection = mapper.readTree(json).get("reflection").asText();
        List<String> emotions = em.createQuery("""
                select e.emotionCode from AnalysisEmotion e where e.analysisId=:id order by e.rank
                """, String.class).setParameter("id", analysisId).getResultList();
        List<String> topics = em.createQuery("""
                select t.topicCode from AnalysisTopic t where t.analysisId=:id order by t.rank
                """, String.class).setParameter("id", analysisId).getResultList();
        return new AnalysisView(entryId, version, status, analysisId,
                analysis.sentimentLabel, analysis.sentimentScore, emotions, topics, reflection);
    }
}
