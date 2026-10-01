package com.mylog.analysis.infrastructure.persistence;

import com.mylog.TestcontainersConfiguration;
import com.mylog.identity.application.VerificationDelivery;
import com.mylog.journal.application.JournalService;
import com.mylog.journal.application.command.CreateJournalEntryCommand;
import com.mylog.journal.application.command.UpdateJournalEntryCommand;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.outbox.OutboxWorker;
import com.mylog.safety.application.RiskClassifier;
import com.mylog.safety.application.RiskLevel;
import com.mylog.user.application.UserProfileUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {"mylog.identity.enabled=true", "mylog.jobs.enabled=true",
        "mylog.ai.fake-enabled=true", "mylog.jobs.poll-delay-ms=600000",
        "spring.mail.host=localhost", "MYLOG_MAIL_FROM=noreply@mylog.local",
        "logging.level.root=WARN"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AiPipelineIntegrationTest {
    @Autowired JournalService journal;
    @Autowired UserProfileUseCase users;
    @Autowired OutboxWorker outbox;
    @Autowired AiJobWorker jobs;
    @Autowired SensitiveDataCipher cipher;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean RiskClassifier classifier;
    @MockitoBean VerificationDelivery delivery;

    @Test void approvedPolicyAndConsentProduceEncryptedOwnerScopedReflection() throws Exception {
        UUID owner = user();
        UUID stranger = user();
        approve(owner);
        users.decideConsent(owner, "AI_PROCESSING", "v1", true);
        var created = journal.create(owner, command("A private synthetic day"), "m3-create-" + UUID.randomUUID());
        assertEquals("PENDING", created.analysisStatus());
        outbox.poll();
        assertEquals(1, jdbc.queryForObject("select count(*) from ai_jobs where aggregate_id=?", Integer.class, created.id()));
        jobs.poll();
        assertEquals("ANALYZED", journal.get(owner, created.id()).analysisStatus());
        assertEquals(1, jdbc.queryForObject("select count(*) from ai_analyses where journal_entry_id=? and status='SUCCEEDED'", Integer.class, created.id()));
        byte[] encrypted = jdbc.queryForObject("select encrypted_output from ai_analyses where journal_entry_id=?", byte[].class, created.id());
        assertFalse(new String(encrypted, StandardCharsets.UTF_8).contains("suy ngẫm"));
        mvc.perform(get("/api/v1/journal-entries/{id}/analysis", created.id())
                .with(jwt().jwt(jwt -> jwt.subject(owner.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ANALYZED"))
                .andExpect(jsonPath("$.reflection").isNotEmpty());
        mvc.perform(get("/api/v1/journal-entries/{id}/analysis", created.id())
                .with(jwt().jwt(jwt -> jwt.subject(stranger.toString()))))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/journal-entries/{id}/analysis", created.id()))
                .andExpect(status().isUnauthorized());
    }

    @Test void changedContentCannotActivateOldJob() {
        UUID owner = user();
        approve(owner);
        users.decideConsent(owner, "AI_PROCESSING", "v1", true);
        var created = journal.create(owner, command("First synthetic version"), "m3-create-" + UUID.randomUUID());
        outbox.poll();
        var updated = journal.update(owner, created.id(), new UpdateJournalEntryCommand(
                "Second synthetic version", null, null, null, null, null, null, null, null, null), created.version());
        jobs.poll();
        assertEquals("ANALYSIS_OUTDATED", journal.get(owner, created.id()).analysisStatus());
        assertEquals(0, jdbc.queryForObject("select count(*) from ai_analyses where journal_entry_id=? and status='SUCCEEDED'", Integer.class, created.id()));
        outbox.poll();
        jobs.poll();
        assertEquals("ANALYZED", journal.get(owner, created.id()).analysisStatus());
        assertEquals(updated.contentVersion(), jdbc.queryForObject("select content_version from ai_analyses where id=(select latest_analysis_id from journal_entries where id=?)", Integer.class, created.id()));
    }

    @Test void consentIsCheckedAtExecutionAndRetryAfterGrantIsRateLimited() throws Exception {
        UUID owner = user();
        approve(owner);
        var created = journal.create(owner, command("Synthetic consent check"), "m3-create-" + UUID.randomUUID());
        outbox.poll();
        jobs.poll();
        assertEquals("NOT_REQUESTED", journal.get(owner, created.id()).analysisStatus());
        assertEquals(0, jdbc.queryForObject("select count(*) from ai_analyses where journal_entry_id=?", Integer.class, created.id()));
        users.decideConsent(owner, "AI_PROCESSING", "v1", true);
        mvc.perform(post("/api/v1/journal-entries/{id}/analysis:retry", created.id())
                .with(jwt().jwt(jwt -> jwt.subject(owner.toString()))))
                .andExpect(status().isTooManyRequests());
        jdbc.update("update ai_jobs set finished_at=? where aggregate_id=?",
                Timestamp.from(Instant.now().minusSeconds(61)), created.id());
        mvc.perform(post("/api/v1/journal-entries/{id}/analysis:retry", created.id())
                .with(jwt().jwt(jwt -> jwt.subject(owner.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING"));
        assertEquals("PENDING", jdbc.queryForObject("select status from ai_jobs where aggregate_id=?", String.class, created.id()));
        jobs.poll();
        assertEquals("SUCCEEDED", jdbc.queryForObject("select status from ai_jobs where aggregate_id=?", String.class, created.id()));
        assertEquals("ANALYZED", journal.get(owner, created.id()).analysisStatus());
    }

    @Test void parallelPollsKeepOneJobAndOneActiveAnalysis() {
        UUID owner = user();
        approve(owner);
        users.decideConsent(owner, "AI_PROCESSING", "v1", true);
        var created = journal.create(owner, command("Synthetic parallel workers"), "m3-create-" + UUID.randomUUID());
        CompletableFuture.allOf(CompletableFuture.runAsync(outbox::poll),
                CompletableFuture.runAsync(outbox::poll)).join();
        assertEquals(1, jdbc.queryForObject("select count(*) from ai_jobs where aggregate_id=?", Integer.class, created.id()));
        CompletableFuture.allOf(CompletableFuture.runAsync(jobs::poll),
                CompletableFuture.runAsync(jobs::poll)).join();
        assertEquals(1, jdbc.queryForObject("select count(*) from ai_analyses where journal_entry_id=? and status='SUCCEEDED'", Integer.class, created.id()));
        assertEquals("ANALYZED", journal.get(owner, created.id()).analysisStatus());
    }

    @Test void safetyRescreenCanQueueAfterClassifierAndPolicyBecomeAvailable() {
        UUID owner = user();
        users.decideConsent(owner, "AI_PROCESSING", "v1", true);
        var created = journal.create(owner, command("Synthetic rescreen"), "m3-create-" + UUID.randomUUID());
        assertEquals("BLOCKED_BY_SAFETY", created.analysisStatus());
        approve(owner);
        outbox.poll();
        assertEquals(1, jdbc.queryForObject("select count(*) from outbox_events where aggregate_id=? and event_type='SafetyRescreenRequested' and status='PUBLISHED'", Integer.class, created.id()));
        jobs.poll();
        assertEquals("ANALYZED", journal.get(owner, created.id()).analysisStatus());
        assertEquals(1, jdbc.queryForObject("select count(*) from ai_jobs where aggregate_id=?", Integer.class, created.id()));
    }

    @Test void expiredLeasesAreReclaimedWithoutLosingJournal() {
        UUID owner = user();
        approve(owner);
        users.decideConsent(owner, "AI_PROCESSING", "v1", true);
        var created = journal.create(owner, command("Synthetic lease recovery"), "m3-create-" + UUID.randomUUID());
        jdbc.update("""
                update outbox_events set status='PROCESSING', locked_by='crashed-worker',
                    locked_at=?, lease_expires_at=? where aggregate_id=?
                """, Timestamp.from(Instant.now().minusSeconds(120)),
                Timestamp.from(Instant.now().minusSeconds(60)), created.id());
        outbox.poll();
        assertEquals(1, jdbc.queryForObject("select count(*) from ai_jobs where aggregate_id=?", Integer.class, created.id()));
        jdbc.update("""
                update ai_jobs set status='PROCESSING', locked_by='crashed-worker',
                    locked_at=?, lease_expires_at=? where aggregate_id=?
                """, Timestamp.from(Instant.now().minusSeconds(120)),
                Timestamp.from(Instant.now().minusSeconds(60)), created.id());
        jobs.poll();
        assertEquals("ANALYZED", journal.get(owner, created.id()).analysisStatus());
    }

    @Test void editingAnAnalyzedEntryInvalidatesOldResult() {
        UUID owner = user();
        approve(owner);
        users.decideConsent(owner, "AI_PROCESSING", "v1", true);
        var created = journal.create(owner, command("Synthetic old result"), "m3-create-" + UUID.randomUUID());
        outbox.poll();
        jobs.poll();
        var analyzed = journal.get(owner, created.id());
        assertEquals("ANALYZED", analyzed.analysisStatus());
        journal.update(owner, created.id(), new UpdateJournalEntryCommand(
                "Synthetic edited result", null, null, null, null, null, null, null, null, null), analyzed.version());
        assertNull(jdbc.queryForObject("select latest_analysis_id from journal_entries where id=?", UUID.class, created.id()));
        outbox.poll();
        assertEquals(1, jdbc.queryForObject("select count(*) from ai_analyses where journal_entry_id=? and status='STALE'", Integer.class, created.id()));
        jobs.poll();
        assertEquals("ANALYZED", journal.get(owner, created.id()).analysisStatus());
        assertEquals(1, jdbc.queryForObject("select count(*) from ai_analyses where journal_entry_id=? and status='SUCCEEDED'", Integer.class, created.id()));
    }

    private void approve(UUID approver) {
        Instant now = Instant.now();
        String config = """
                {"allowOrdinaryAnalysis":true,"ruleVersion":"m2-curated-draft-v1",
                 "classifierProvider":"synthetic-test","classifierVersion":"v1","minConfidence":0.9}
                """;
        jdbc.update("""
                insert into safety_policy_versions(id,version,status,config,approved_by,approved_at,
                    effective_from,created_at)
                values (?,?,'APPROVED',cast(? as jsonb),?,?,?,?)
                """, UUID.randomUUID(), "m3-" + UUID.randomUUID().toString().substring(0, 12), config, approver,
                Timestamp.from(now.minusSeconds(2)), Timestamp.from(now.minusSeconds(2)), Timestamp.from(now));
        when(classifier.classify(anyString())).thenReturn(Optional.of(new RiskClassifier.Classification(
                "synthetic-test", "v1", RiskLevel.NORMAL, new BigDecimal("0.99"))));
    }

    private CreateJournalEntryCommand command(String title) {
        var content = mapper.readTree("""
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"A calm synthetic day"}]}]}
                """);
        return new CreateJournalEntryCommand(title, content, null, Instant.now(), "Asia/Ho_Chi_Minh",
                "calm", BigDecimal.valueOf(7), null, null, null);
    }

    private UUID user() {
        UUID id = UUID.randomUUID();
        String email = "m3-" + id + "@example.test";
        var encrypted = cipher.encrypt("users.email", id, id, email);
        Instant now = Instant.now();
        jdbc.update("""
                insert into users(id,email_lookup_hash,encrypted_email,email_iv,email_wrapped_key,email_key_version,
                    password_hash,auth_provider,status,created_at,updated_at)
                values (?,?,?,?,?,?,'test','LOCAL','ACTIVE',?,?)
                """, id, cipher.lookupHash(email), encrypted.ciphertext(), encrypted.iv(),
                encrypted.wrappedKey(), encrypted.keyVersion(), Timestamp.from(now), Timestamp.from(now));
        users.createDefault(id, "Asia/Ho_Chi_Minh", "vi", "v1", "v1", now);
        return id;
    }
}
