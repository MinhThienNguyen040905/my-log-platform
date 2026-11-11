package com.mylog.insight.infrastructure.persistence;

import com.mylog.insight.application.DailyAggregateSource;
import com.mylog.insight.application.query.ActivitySignal;
import com.mylog.insight.application.query.DailyMetric;
import com.mylog.insight.application.query.EntrySignal;
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

    @Override @Transactional(readOnly = true)
    public List<EntrySignal> entrySignals(UUID userId, LocalDate from, LocalDate to) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT j.id, j.local_date, j.mood_score, j.stress_score,
                    (SELECT string_agg(e.emotion_code, ',' ORDER BY e.rank) FROM analysis_emotions e
                        WHERE e.analysis_id=a.id),
                    (SELECT string_agg(t.topic_code, ',' ORDER BY t.rank) FROM analysis_topics t
                        WHERE t.analysis_id=a.id),
                    (SELECT string_agg(g.tag_id::text, ',') FROM journal_entry_tags g
                        WHERE g.journal_entry_id=j.id)
                FROM journal_entries j
                LEFT JOIN ai_analyses a ON a.id=j.latest_analysis_id AND j.analysis_status='ANALYZED'
                    AND a.user_id=j.user_id AND a.journal_entry_id=j.id
                    AND a.status='SUCCEEDED' AND a.content_version=j.content_version
                WHERE j.user_id=?1 AND j.local_date BETWEEN ?2 AND ?3
                    AND j.deleted_at IS NULL AND j.entry_status='SAVED'
                ORDER BY j.local_date, j.occurred_at, j.id
                """).setParameter(1, userId).setParameter(2, from).setParameter(3, to).getResultList();
        List<EntrySignal> signals = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            signals.add(new EntrySignal((UUID) row[0], (LocalDate) row[1], decimal(row[2]), decimal(row[3]),
                    split(row[4]), split(row[5]), split(row[6]).stream().map(UUID::fromString).toList()));
        }
        return signals;
    }

    @Override @Transactional(readOnly = true)
    public List<ActivitySignal> activities(UUID userId, LocalDate from, LocalDate to) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT c.local_date, a.activity_code
                FROM daily_checkins c JOIN checkin_activities a ON a.checkin_id=c.id
                WHERE c.user_id=?1 AND c.local_date BETWEEN ?2 AND ?3
                ORDER BY c.local_date, a.activity_code
                """).setParameter(1, userId).setParameter(2, from).setParameter(3, to).getResultList();
        List<ActivitySignal> signals = new ArrayList<>(rows.size());
        for (Object[] row : rows) signals.add(new ActivitySignal((LocalDate) row[0], (String) row[1]));
        return signals;
    }

    @Override @Transactional(readOnly = true)
    public boolean hasElevatedRisk(UUID userId, LocalDate from, LocalDate to) {
        Number count = (Number) em.createNativeQuery("""
                SELECT count(*) FROM journal_entries
                WHERE user_id=?1 AND local_date BETWEEN ?2 AND ?3 AND deleted_at IS NULL
                    AND risk_level IN ('HIGH','CRITICAL')
                """).setParameter(1, userId).setParameter(2, from).setParameter(3, to).getSingleResult();
        return count.longValue() > 0;
    }

    private static List<String> split(Object value) {
        return value == null ? List.of() : List.of(((String) value).split(","));
    }

    private static BigDecimal decimal(Object value) { return value == null ? null : (BigDecimal) value; }
    private static Integer integer(Object value) { return value == null ? null : ((Number) value).intValue(); }
}
