package com.mylog.export.infrastructure.persistence;

import com.mylog.export.application.ExportStore;
import com.mylog.export.application.query.ExportView;
import com.mylog.platform.crypto.SensitiveDataCipher;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaExportStore implements ExportStore {
    private final EntityManager em;
    JpaExportStore(EntityManager em) { this.em = em; }
    @Override public ExportView create(UUID id, UUID userId, String format, Instant now) {
        ExportRequest request = new ExportRequest();
        request.id=id; request.userId=userId; request.format=format; request.status="PENDING";
        request.createdAt=now; request.availableAt=now;
        em.persist(request); em.flush();
        return view(request);
    }
    @Override public Optional<ExportView> find(UUID userId, UUID id) {
        return em.createQuery("select r from ExportRequest r where r.id=:id and r.userId=:user", ExportRequest.class)
                .setParameter("id",id).setParameter("user",userId).getResultStream().findFirst().map(this::view);
    }
    @Override public Optional<Work> claim(Instant now) {
        var rows = em.createNativeQuery("""
                SELECT id FROM export_requests WHERE
                  (status IN ('PENDING','FAILED') AND attempt < 3 AND available_at <= :now)
                  OR (status='PROCESSING' AND attempt < 3 AND lease_expires_at < :now)
                ORDER BY created_at LIMIT 1 FOR UPDATE SKIP LOCKED
                """, UUID.class).setParameter("now",now).getResultList();
        if (rows.isEmpty()) return Optional.empty();
        ExportRequest request = em.find(ExportRequest.class, rows.getFirst());
        request.status="PROCESSING"; request.attempt++; request.leaseExpiresAt=now.plusSeconds(300);
        return Optional.of(new Work(request.id,request.userId,request.format,request.attempt));
    }
    @Override public void ready(UUID id, UUID userId, SensitiveDataCipher.Encrypted encrypted, byte[] sha,
                                long size, Instant now, Instant expiresAt) {
        ExportRequest request = em.find(ExportRequest.class,id);
        if (request == null || !request.userId.equals(userId) || !"PROCESSING".equals(request.status)) return;
        Number active=(Number)em.createNativeQuery("SELECT count(*) FROM users WHERE id=:user AND status='ACTIVE'")
                .setParameter("user",userId).getSingleResult();
        if (active.intValue()!=1) throw new IllegalStateException("Account not active");
        request.encryptedFile=encrypted.ciphertext(); request.fileIv=encrypted.iv();
        request.fileWrappedKey=encrypted.wrappedKey(); request.fileKeyVersion=encrypted.keyVersion();
        request.fileSha256=sha; request.fileSizeBytes=size; request.completedAt=now;
        request.expiresAt=expiresAt; request.status="READY"; request.leaseExpiresAt=null;
    }
    @Override public void fail(UUID id, Instant retryAt, boolean terminal) {
        ExportRequest request = em.find(ExportRequest.class,id);
        if (request == null || !"PROCESSING".equals(request.status)) return;
        request.status="FAILED"; request.lastErrorCode=terminal ? "EXPORT_FAILED" : "RETRYABLE";
        if (terminal) request.attempt=3;
        request.availableAt=retryAt; request.leaseExpiresAt=null;
    }
    @Override public Optional<Artifact> artifact(UUID userId, UUID id, Instant now) {
        return em.createQuery("""
                select r from ExportRequest r where r.id=:id and r.userId=:user and r.status='READY' and r.expiresAt>:now
                """,ExportRequest.class).setParameter("id",id).setParameter("user",userId)
                .setParameter("now",now).getResultStream().findFirst().map(r -> new Artifact(r.format,
                        new SensitiveDataCipher.Encrypted(r.encryptedFile,r.fileIv,r.fileWrappedKey,r.fileKeyVersion),
                        r.fileSha256,r.expiresAt));
    }
    @Override public void expire(Instant now) {
        em.createNativeQuery("""
                UPDATE export_requests SET status='EXPIRED', encrypted_file=NULL, file_iv=NULL,
                file_wrapped_key=NULL, file_key_version=NULL, file_sha256=NULL, file_size_bytes=NULL
                WHERE status='READY' AND expires_at<=:now
                """).setParameter("now",now).executeUpdate();
        em.createNativeQuery("DELETE FROM export_requests WHERE status='EXPIRED' AND expires_at<:cutoff")
                .setParameter("cutoff",now.minusSeconds(30L*86400)).executeUpdate();
    }
    private ExportView view(ExportRequest r) { return new ExportView(r.id,r.format,r.status,r.createdAt,r.completedAt,r.expiresAt); }
}
