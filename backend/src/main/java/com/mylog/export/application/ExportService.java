package com.mylog.export.application;

import com.mylog.export.application.query.ExportView;
import com.mylog.identity.application.IdentityService;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.id.IdGenerator;
import com.mylog.platform.web.ConflictException;
import com.mylog.platform.web.InvalidRequestException;
import com.mylog.platform.web.ResourceNotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class ExportService {
    private final ExportStore store;
    private final ExportSnapshot snapshot;
    private final ExportDocument document;
    private final IdentityService identity;
    private final SensitiveDataCipher cipher;
    private final IdGenerator ids;
    private final Clock clock;
    private final TransactionTemplate transactions;
    private final boolean workerEnabled;
    public ExportService(ExportStore store, ExportSnapshot snapshot, ExportDocument document,
                         IdentityService identity, SensitiveDataCipher cipher, IdGenerator ids, Clock clock,
                         TransactionTemplate transactions,
                         @Value("${mylog.exports.enabled:true}") boolean workerEnabled) {
        this.store=store;this.snapshot=snapshot;this.document=document;this.identity=identity;
        this.cipher=cipher;this.ids=ids;this.clock=clock;this.transactions=transactions;
        this.workerEnabled=workerEnabled;
    }
    @Transactional
    public ExportView request(UUID userId, String format) {
        if (!"CSV".equals(format) && !"PDF".equals(format)) throw new InvalidRequestException();
        store.expire(clock.instant());
        try { return store.create(ids.next(),userId,format,clock.instant()); }
        catch (DataIntegrityViolationException e) { throw new ConflictException("An export request is already active."); }
    }
    @Transactional(readOnly = true)
    public ExportView get(UUID userId, UUID id) {
        return store.find(userId,id).orElseThrow(() -> new ResourceNotFoundException("Export not found."));
    }
    public record DownloadGrant(String url, Instant expiresAt) {}
    public record Download(byte[] bytes, String format) {}
    @Transactional(readOnly = true)
    public DownloadGrant authorizeDownload(UUID userId, UUID id, String password) {
        identity.reauthenticate(userId,password);
        var artifact = store.artifact(userId,id,clock.instant())
                .orElseThrow(() -> new ResourceNotFoundException("Export not found."));
        Instant expires=clock.instant().plusSeconds(60);
        String signature=sign(userId,id,expires.getEpochSecond(),artifact.sha256());
        return new DownloadGrant("/api/v1/exports/"+id+"/file?expires="+expires.getEpochSecond()+"&signature="+signature,expires);
    }
    @Transactional(readOnly = true)
    public Download download(UUID userId, UUID id, long expires, String signature) {
        Instant now=clock.instant();
        if (expires<=now.getEpochSecond() || expires>now.plusSeconds(60).getEpochSecond())
            throw new ResourceNotFoundException("Export not found.");
        var artifact=store.artifact(userId,id,now)
                .orElseThrow(() -> new ResourceNotFoundException("Export not found."));
        String expected=sign(userId,id,expires,artifact.sha256());
        if (signature==null || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                signature.getBytes(StandardCharsets.US_ASCII))) throw new ResourceNotFoundException("Export not found.");
        byte[] bytes=Base64.getDecoder().decode(cipher.decrypt("export_requests.file",userId,id,artifact.encrypted()));
        try {
            if (!MessageDigest.isEqual(MessageDigest.getInstance("SHA-256").digest(bytes),artifact.sha256()))
                throw new IllegalStateException("Export integrity failure");
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
        return new Download(bytes,artifact.format());
    }
    private String sign(UUID userId, UUID id, long expires, byte[] sha) {
        String input="export:"+userId+":"+id+":"+expires+":"+HexFormat.of().formatHex(sha);
        return HexFormat.of().formatHex(cipher.tokenHash(input));
    }
    @Scheduled(fixedDelayString = "${mylog.exports.poll-delay-ms:5000}")
    public void poll() {
        if (!workerEnabled) return;
        transactions.executeWithoutResult(s -> store.expire(clock.instant()));
        ExportStore.Work work=transactions.execute(s -> store.claim(clock.instant()).orElse(null));
        if (work==null) return;
        try {
            if (!identity.isActive(work.userId())) throw new IllegalStateException("Account not active");
            byte[] file=document.render(work.format(),snapshot.capture(work.userId()));
            if (file.length>5_000_000) throw new IllegalStateException("Export size limit exceeded");
            byte[] sha=MessageDigest.getInstance("SHA-256").digest(file);
            var encrypted=cipher.encrypt("export_requests.file",work.userId(),work.id(),
                    Base64.getEncoder().encodeToString(file));
            transactions.executeWithoutResult(s -> store.ready(work.id(),work.userId(),encrypted,sha,file.length,
                    clock.instant(),clock.instant().plusSeconds(86400)));
        } catch (Exception e) {
            transactions.executeWithoutResult(s -> store.fail(work.id(),clock.instant().plusSeconds(
                    Math.min(3600,30L << Math.min(work.attempt(),6))),work.attempt()>=3));
        }
    }
}
