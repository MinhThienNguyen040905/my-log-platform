package com.mylog.reporting.infrastructure.persistence;

import com.mylog.reporting.infrastructure.persistence.entity.Report;
import com.mylog.reporting.infrastructure.persistence.entity.ReportEvidence;

import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.id.IdGenerator;
import com.mylog.platform.web.InvalidRequestException;
import com.mylog.platform.web.ResourceNotFoundException;
import com.mylog.reporting.application.ReportStore;
import com.mylog.reporting.application.query.ReportEvidenceView;
import com.mylog.reporting.application.query.ReportPage;
import com.mylog.reporting.application.query.ReportView;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaReportStore implements ReportStore {
    private final EntityManager em;
    private final IdGenerator ids;
    private final SensitiveDataCipher cipher;

    JpaReportStore(EntityManager em, IdGenerator ids, SensitiveDataCipher cipher) {
        this.em = em; this.ids = ids; this.cipher = cipher;
    }

    @Override @Transactional(readOnly = true)
    public List<UserZone> usersAfter(UUID cursor, int limit) {
        String sql = "SELECT user_id,timezone FROM user_profiles "
                + (cursor == null ? "" : "WHERE user_id>?1 ") + "ORDER BY user_id LIMIT " + limit;
        var query = em.createNativeQuery(sql);
        if (cursor != null) query.setParameter(1, cursor);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(row -> new UserZone((UUID) row[0], row[1].toString())).toList();
    }

    @Override @Transactional
    public UUID enqueue(UUID userId, String type, LocalDate from, LocalDate to,
                        String timezone, boolean regenerate, Instant now) {
        if (from.isAfter(to) || !List.of("WEEKLY", "MONTHLY").contains(type)) throw new InvalidRequestException();
        String key = userId + ":" + type + ":" + from;
        em.createNativeQuery("SELECT pg_advisory_xact_lock(hashtextextended(?1,0))")
                .setParameter(1, key).getSingleResult();
        List<Report> existing = em.createQuery("""
                select r from Report r where r.userId=:userId and r.reportType=:type and r.periodStart=:from
                order by r.version desc
                """, Report.class).setParameter("userId", userId).setParameter("type", type)
                .setParameter("from", from).setMaxResults(1).getResultList();
        if (!regenerate && !existing.isEmpty()) return existing.getFirst().id;
        Report report = new Report();
        report.id = ids.next();
        report.userId = userId;
        report.reportType = type;
        report.periodStart = from;
        report.periodEnd = to;
        report.timezone = timezone;
        report.version = existing.isEmpty() ? 1 : existing.getFirst().version + 1;
        report.status = "PENDING";
        report.sampleSize = 0;
        report.metricsSnapshot = Map.of();
        report.generatedBy = "RULE";
        report.availableAt = now;
        report.createdAt = now;
        em.persist(report);
        return report.id;
    }

    @Override public Task claim(String workerId, Instant now) {
        @SuppressWarnings("unchecked")
        List<UUID> ids = em.createNativeQuery("""
                SELECT id FROM reports WHERE (status IN ('PENDING','FAILED') AND available_at<=?1)
                    OR (status='PROCESSING' AND lease_expires_at<?1)
                ORDER BY available_at, created_at FOR UPDATE SKIP LOCKED LIMIT 1
                """, UUID.class).setParameter(1, now).getResultList();
        if (ids.isEmpty()) return null;
        Report report = em.find(Report.class, ids.getFirst());
        report.status = "PROCESSING";
        report.attempt++;
        report.lockedAt = now;
        report.lockedBy = workerId;
        report.leaseExpiresAt = now.plusSeconds(300);
        em.flush();
        return new Task(report.id, report.userId, report.reportType, report.periodStart,
                report.periodEnd, report.timezone, report.version);
    }

    @Override public void complete(UUID reportId, String workerId, Result result, Instant now) {
        Report report = em.find(Report.class, reportId);
        if (!"PROCESSING".equals(report.status) || !workerId.equals(report.lockedBy)
                || !report.leaseExpiresAt.isAfter(now)) return;
        report.sampleSize = result.sampleSize();
        report.metricsSnapshot = result.metrics();
        var encrypted = cipher.encrypt("reports.narrative", report.userId, report.id, result.narrative());
        report.encryptedNarrative = encrypted.ciphertext();
        report.narrativeIv = encrypted.iv();
        report.narrativeWrappedKey = encrypted.wrappedKey();
        report.narrativeKeyVersion = encrypted.keyVersion();
        report.status = "READY";
        report.completedAt = now;
        report.lockedAt = null; report.lockedBy = null; report.leaseExpiresAt = null;
        report.lastErrorCode = null;
        for (Evidence value : result.evidence()) {
            ReportEvidence evidence = new ReportEvidence();
            evidence.id = ids.next();
            evidence.reportId = reportId;
            evidence.sourceType = "CHECKIN".equals(value.source()) ? "CHECKIN" : "JOURNAL";
            evidence.sourceId = value.sourceId();
            evidence.evidenceDate = value.date();
            evidence.metricName = value.metric();
            evidence.metricValue = value.value();
            evidence.metadata = Map.of("source", value.source());
            evidence.createdAt = now;
            em.persist(evidence);
        }
    }

    @Override public void fail(UUID reportId, String workerId, Instant now) {
        Report report = em.find(Report.class, reportId);
        if (!"PROCESSING".equals(report.status) || !workerId.equals(report.lockedBy)) return;
        boolean terminal = report.attempt >= 5;
        report.status = terminal ? "DEAD" : "FAILED";
        report.lastErrorCode = "GENERATION_FAILED";
        long delay = Math.min(3600, 1L << Math.min(report.attempt, 10));
        report.availableAt = now.plusSeconds(delay + ThreadLocalRandom.current().nextLong(delay / 4 + 1));
        report.lockedAt = null; report.lockedBy = null; report.leaseExpiresAt = null;
    }

    @Override @Transactional(readOnly = true)
    public ReportPage list(UUID userId, String type, String cursor, int limit) {
        Cursor decoded = decode(cursor);
        StringBuilder jpql = new StringBuilder("select r from Report r where r.userId=:userId");
        if (type != null) jpql.append(" and r.reportType=:type");
        if (decoded != null) jpql.append(" and (r.periodEnd<:cursorDate or (r.periodEnd=:cursorDate and r.id<:cursorId))");
        jpql.append(" order by r.periodEnd desc,r.id desc");
        var query = em.createQuery(jpql.toString(), Report.class).setParameter("userId", userId)
                .setMaxResults(limit + 1);
        if (type != null) query.setParameter("type", type);
        if (decoded != null) query.setParameter("cursorDate", decoded.date()).setParameter("cursorId", decoded.id());
        List<Report> rows = query.getResultList();
        boolean more = rows.size() > limit;
        List<Report> page = more ? rows.subList(0, limit) : rows;
        List<ReportView> items = page.stream().map(report -> view(report, false)).toList();
        return new ReportPage(items, more ? encode(page.getLast()) : null);
    }

    @Override @Transactional(readOnly = true)
    public ReportView get(UUID userId, UUID reportId) {
        Report report = em.createQuery("""
                select r from Report r where r.id=:id and r.userId=:userId
                """, Report.class).setParameter("id", reportId).setParameter("userId", userId)
                .getResultStream().findFirst().orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo cáo."));
        return view(report, true);
    }

    private ReportView view(Report report, boolean withEvidence) {
        String narrative = report.encryptedNarrative == null ? null : cipher.decrypt("reports.narrative",
                report.userId, report.id, new SensitiveDataCipher.Encrypted(report.encryptedNarrative,
                        report.narrativeIv, report.narrativeWrappedKey, report.narrativeKeyVersion));
        List<ReportEvidenceView> evidence = withEvidence ? em.createQuery("""
                select e from ReportEvidence e where e.reportId=:id order by e.evidenceDate,e.metricName
                """, ReportEvidence.class).setParameter("id", report.id).getResultList().stream()
                .map(e -> new ReportEvidenceView(e.sourceType, e.evidenceDate, e.metricName, e.metricValue))
                .toList() : List.of();
        return new ReportView(report.id, report.reportType, report.periodStart, report.periodEnd,
                report.timezone, report.version, report.status, report.sampleSize,
                report.metricsSnapshot, narrative, evidence, report.createdAt, report.completedAt);
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

    private static String encode(Report report) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (report.periodEnd + "|" + report.id).getBytes(StandardCharsets.UTF_8));
    }
}
