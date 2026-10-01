package com.mylog.insight.infrastructure.persistence;

import com.mylog.insight.application.DailyAggregateSource;
import com.mylog.insight.application.query.DailyMetric;
import com.mylog.insight.application.query.TopCode;
import com.mylog.platform.web.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaDailyAggregateSource implements DailyAggregateSource {
    private final EntityManager em;

    JpaDailyAggregateSource(EntityManager em) { this.em = em; }

    @Override @Transactional(readOnly = true)
    public String timezone(UUID userId) {
        @SuppressWarnings("unchecked")
        List<String> values = em.createNativeQuery("SELECT timezone FROM user_profiles WHERE user_id=?1", String.class)
                .setParameter(1, userId).getResultList();
        if (values.isEmpty()) throw new ResourceNotFoundException("Không tìm thấy hồ sơ.");
        return values.getFirst();
    }

    @Override @Transactional(readOnly = true)
    public List<DailyMetric> daily(UUID userId, LocalDate from, LocalDate to) {
        Map<LocalDate, DailyMetric> byDate = new HashMap<>();
        @SuppressWarnings("unchecked")
        List<Object[]> journal = em.createNativeQuery("""
                SELECT DISTINCT ON (local_date) id, local_date, mood_score, stress_score,
                    energy_score, sleep_minutes, count(*) OVER (PARTITION BY local_date)
                FROM journal_entries
                WHERE user_id=?1 AND local_date BETWEEN ?2 AND ?3
                    AND deleted_at IS NULL AND entry_status='SAVED'
                ORDER BY local_date, occurred_at DESC, id DESC
                """).setParameter(1, userId).setParameter(2, from).setParameter(3, to).getResultList();
        for (Object[] row : journal) {
            LocalDate date = (LocalDate) row[1];
            byDate.put(date, new DailyMetric(date, "JOURNAL_FALLBACK", (UUID) row[0],
                    ((Number) row[6]).intValue(), decimal(row[2]), decimal(row[3]),
                    decimal(row[4]), integer(row[5])));
        }
        @SuppressWarnings("unchecked")
        List<Object[]> checkins = em.createNativeQuery("""
                SELECT id, local_date, mood_score, stress_score, energy_score, sleep_minutes
                FROM daily_checkins WHERE user_id=?1 AND local_date BETWEEN ?2 AND ?3
                ORDER BY local_date
                """).setParameter(1, userId).setParameter(2, from).setParameter(3, to).getResultList();
        for (Object[] row : checkins) {
            LocalDate date = (LocalDate) row[1];
            DailyMetric fallback = byDate.get(date);
            byDate.put(date, new DailyMetric(date, "CHECKIN", (UUID) row[0],
                    fallback == null ? 0 : fallback.journalCount(), decimal(row[2]),
                    decimal(row[3]), decimal(row[4]), integer(row[5])));
        }
        return byDate.values().stream().sorted(java.util.Comparator.comparing(DailyMetric::date)).toList();
    }

    @Override @Transactional(readOnly = true)
    public int currentJournalStreak(UUID userId, LocalDate today) {
        @SuppressWarnings("unchecked")
        List<LocalDate> dates = em.createNativeQuery("""
                SELECT DISTINCT local_date FROM journal_entries
                WHERE user_id=?1 AND local_date<=?2 AND deleted_at IS NULL AND entry_status='SAVED'
                ORDER BY local_date DESC
                """, LocalDate.class).setParameter(1, userId).setParameter(2, today).getResultList();
        if (dates.isEmpty()) return 0;
        LocalDate expected = dates.getFirst().equals(today) ? today : today.minusDays(1);
        if (!dates.getFirst().equals(expected)) return 0;
        int count = 0;
        for (LocalDate date : dates) {
            if (!date.equals(expected)) break;
            count++;
            expected = expected.minusDays(1);
        }
        return count;
    }

    @Override @Transactional(readOnly = true)
    public List<TopCode> topEmotions(UUID userId, LocalDate from, LocalDate to, int limit) {
        return top("""
                SELECT e.emotion_code, count(*), avg(e.score)
                FROM journal_entries j JOIN ai_analyses a ON a.id=j.latest_analysis_id
                JOIN analysis_emotions e ON e.analysis_id=a.id
                WHERE j.user_id=?1 AND j.local_date BETWEEN ?2 AND ?3
                    AND j.deleted_at IS NULL AND j.analysis_status='ANALYZED'
                    AND a.user_id=?1 AND a.journal_entry_id=j.id
                    AND a.status='SUCCEEDED' AND a.content_version=j.content_version
                GROUP BY e.emotion_code ORDER BY count(*) DESC, e.emotion_code LIMIT ?4
                """, userId, from, to, limit);
    }

    @Override @Transactional(readOnly = true)
    public List<TopCode> topTopics(UUID userId, LocalDate from, LocalDate to, int limit) {
        return top("""
                SELECT t.topic_code, count(*), avg(t.score)
                FROM journal_entries j JOIN ai_analyses a ON a.id=j.latest_analysis_id
                JOIN analysis_topics t ON t.analysis_id=a.id
                WHERE j.user_id=?1 AND j.local_date BETWEEN ?2 AND ?3
                    AND j.deleted_at IS NULL AND j.analysis_status='ANALYZED'
                    AND a.user_id=?1 AND a.journal_entry_id=j.id
                    AND a.status='SUCCEEDED' AND a.content_version=j.content_version
                GROUP BY t.topic_code ORDER BY count(*) DESC, t.topic_code LIMIT ?4
                """, userId, from, to, limit);
    }

    private List<TopCode> top(String sql, UUID userId, LocalDate from, LocalDate to, int limit) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql).setParameter(1, userId)
                .setParameter(2, from).setParameter(3, to).setParameter(4, limit).getResultList();
        List<TopCode> codes = new ArrayList<>();
        for (Object[] row : rows)
            codes.add(new TopCode(row[0].toString(), ((Number) row[1]).longValue(), decimal(row[2])));
        return codes;
    }

    private static BigDecimal decimal(Object value) { return value == null ? null : (BigDecimal) value; }
    private static Integer integer(Object value) { return value == null ? null : ((Number) value).intValue(); }
}
