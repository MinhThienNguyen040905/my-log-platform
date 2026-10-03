package com.mylog.insight.infrastructure.persistence;

import com.mylog.insight.infrastructure.persistence.entity.Insight;
import com.mylog.insight.infrastructure.persistence.entity.InsightEvidence;

import com.mylog.insight.application.InsightStore;
import com.mylog.insight.application.query.InsightEvidenceView;
import com.mylog.insight.application.query.InsightPage;
import com.mylog.insight.application.query.InsightView;
import com.mylog.insight.domain.MoodSleepAssociation;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.id.IdGenerator;
import com.mylog.platform.web.InvalidRequestException;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaInsightStore implements InsightStore {
    private static final String VERSION = "sleep-mood-pearson-v1";
    private final EntityManager em;
    private final IdGenerator ids;
    private final SensitiveDataCipher cipher;

    JpaInsightStore(EntityManager em, IdGenerator ids, SensitiveDataCipher cipher) {
        this.em = em; this.ids = ids; this.cipher = cipher;
    }

    @Override public void saveAssociation(UUID userId, LocalDate from, LocalDate to, String timezone,
                                          MoodSleepAssociation.Result result, Instant now) {
        String key = userId + ":sleep-mood:" + from + ":" + to;
        em.createNativeQuery("SELECT pg_advisory_xact_lock(hashtextextended(?1,0))")
                .setParameter(1, key).getSingleResult();
        boolean exists = !em.createQuery("""
                select i.id from Insight i where i.userId=:userId and i.insightType='SLEEP_MOOD_ASSOCIATION'
                    and i.periodStart=:from and i.periodEnd=:to and i.algorithmVersion=:version
                """, UUID.class).setParameter("userId", userId).setParameter("from", from)
                .setParameter("to", to).setParameter("version", VERSION).setMaxResults(1)
                .getResultList().isEmpty();
        if (exists) return;
        Insight insight = new Insight();
        insight.id = ids.next();
        insight.userId = userId;
        insight.insightType = "SLEEP_MOOD_ASSOCIATION";
        insight.periodStart = from;
        insight.periodEnd = to;
        insight.timezone = timezone;
        insight.status = "ACTIVE";
        insight.direction = result.direction();
        insight.strength = result.strength();
        insight.sampleSize = result.sampleSize();
        insight.algorithmVersion = VERSION;
        insight.metricsSnapshot = Map.of("correlation", result.correlation(),
                "averageMood", result.averageMood(), "averageSleepHours", result.averageSleepHours());
        String relationship = switch (result.direction()) {
            case "UP" -> "cùng chiều";
            case "DOWN" -> "ngược chiều";
            default -> "rất yếu";
        };
        String narrative = "Trong " + result.sampleSize()
                + " ngày có đủ dữ liệu, thời lượng ngủ và điểm tâm trạng có mối liên hệ "
                + relationship + ". Mối liên hệ này không chứng minh nguyên nhân.";
        var encrypted = cipher.encrypt("insights.narrative", userId, insight.id, narrative);
        insight.encryptedNarrative = encrypted.ciphertext();
        insight.narrativeIv = encrypted.iv();
        insight.narrativeWrappedKey = encrypted.wrappedKey();
        insight.narrativeKeyVersion = encrypted.keyVersion();
        insight.generatedBy = "STATISTICAL";
        insight.createdAt = now;
        em.persist(insight);
        for (var day : result.evidence()) {
            evidence(insight.id, day.date(), day.source(), day.sourceId(), "MOOD_SCORE",
                    day.moodScore(), now);
            evidence(insight.id, day.date(), day.source(), day.sourceId(), "SLEEP_MINUTES",
                    BigDecimal.valueOf(day.sleepMinutes()), now);
        }
    }

    private void evidence(UUID insightId, LocalDate date, String source, UUID sourceId,
                          String metric, BigDecimal value, Instant now) {
        InsightEvidence evidence = new InsightEvidence();
        evidence.id = ids.next();
        evidence.insightId = insightId;
        evidence.sourceType = "CHECKIN".equals(source) ? "CHECKIN" : "JOURNAL";
        evidence.sourceId = sourceId;
        evidence.evidenceDate = date;
        evidence.metricName = metric;
        evidence.metricValue = value;
        evidence.weight = BigDecimal.ONE;
        evidence.metadata = Map.of("source", source);
        evidence.createdAt = now;
        em.persist(evidence);
    }

    @Override public InsightPage list(UUID userId, LocalDate from, LocalDate to, String cursor, int limit) {
        Cursor decoded = decode(cursor);
        StringBuilder jpql = new StringBuilder("select i from Insight i where i.userId=:userId and i.status='ACTIVE'");
        if (from != null) jpql.append(" and i.periodStart>=:from");
        if (to != null) jpql.append(" and i.periodEnd<=:to");
        if (decoded != null) jpql.append(" and (i.periodEnd<:cursorDate or (i.periodEnd=:cursorDate and i.id<:cursorId))");
        jpql.append(" order by i.periodEnd desc, i.id desc");
        var query = em.createQuery(jpql.toString(), Insight.class).setParameter("userId", userId)
                .setMaxResults(limit + 1);
        if (from != null) query.setParameter("from", from);
        if (to != null) query.setParameter("to", to);
        if (decoded != null) query.setParameter("cursorDate", decoded.date()).setParameter("cursorId", decoded.id());
        List<Insight> rows = query.getResultList();
        boolean more = rows.size() > limit;
        List<Insight> page = more ? rows.subList(0, limit) : rows;
        List<InsightView> items = new ArrayList<>();
        for (Insight insight : page) {
            List<InsightEvidenceView> evidence = em.createQuery("""
                    select e from InsightEvidence e where e.insightId=:id
                    order by e.evidenceDate, e.metricName
                    """, InsightEvidence.class).setParameter("id", insight.id).getResultList().stream()
                    .map(e -> new InsightEvidenceView(e.sourceType, e.evidenceDate, e.metricName, e.metricValue))
                    .toList();
            String narrative = cipher.decrypt("insights.narrative", userId, insight.id,
                    new SensitiveDataCipher.Encrypted(insight.encryptedNarrative, insight.narrativeIv,
                            insight.narrativeWrappedKey, insight.narrativeKeyVersion));
            items.add(new InsightView(insight.id, insight.insightType, insight.periodStart,
                    insight.periodEnd, insight.timezone, insight.direction, insight.strength,
                    insight.sampleSize, insight.algorithmVersion, narrative, evidence));
        }
        String next = more ? encode(page.getLast()) : null;
        return new InsightPage(items, next);
    }

    private record Cursor(LocalDate date, UUID id) {}

    private static Cursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            String value = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = value.split("\\|", 2);
            if (parts.length != 2) throw new IllegalArgumentException();
            return new Cursor(LocalDate.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (RuntimeException e) { throw new InvalidRequestException(); }
    }

    private static String encode(Insight insight) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (insight.periodEnd + "|" + insight.id).getBytes(StandardCharsets.UTF_8));
    }
}
