package com.mylog.journal;

import com.mylog.TestcontainersConfiguration;
import com.mylog.checkin.application.CheckinService;
import com.mylog.checkin.application.command.PutCheckinCommand;
import com.mylog.journal.application.JournalService;
import com.mylog.journal.application.JournalTagService;
import com.mylog.journal.application.JournalStore;
import com.mylog.journal.application.command.CreateJournalEntryCommand;
import com.mylog.journal.application.command.UpdateJournalEntryCommand;
import com.mylog.identity.application.VerificationDelivery;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.web.ResourceNotFoundException;
import com.mylog.safety.application.SafetyPolicyGate;
import com.mylog.safety.application.SafetyResourceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {"mylog.identity.enabled=true", "spring.mail.host=localhost",
        "MYLOG_MAIL_FROM=noreply@mylog.local", "logging.level.root=WARN"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class JournalCheckinIntegrationTest {
    @Autowired JournalService journal;
    @Autowired CheckinService checkins;
    @Autowired JournalTagService tags;
    @Autowired JournalStore journalStore;
    @Autowired SafetyPolicyGate policyGate;
    @Autowired SafetyResourceService safetyResources;
    @Autowired MockMvc mvc;
    @Autowired SensitiveDataCipher cipher;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transactions;
    @Autowired ObjectMapper mapper;
    @MockitoBean VerificationDelivery delivery;

    @Test void journalIsEncryptedOwnerScopedIdempotentAndSafetyGated() {
        UUID owner = user();
        UUID stranger = user();
        var content = mapper.readTree("""
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"I want to die"}]}]}
                """);
        var command = new CreateJournalEntryCommand("A private title", content, null,
                Instant.parse("2026-01-02T12:00:00Z"), "Asia/Ho_Chi_Minh", "sad",
                BigDecimal.valueOf(3), null, null, null);
        var created = journal.create(owner, command, "journal-test-key-123");
        assertEquals("HIGH", created.riskLevel());
        assertEquals("BLOCKED_BY_SAFETY", created.analysisStatus());
        assertEquals(created.id(), journal.create(owner, command, "journal-test-key-123").id());
        assertEquals(1, jdbc.queryForObject("select count(*) from journal_entries where id=?", Integer.class, created.id()));
        byte[] stored = jdbc.queryForObject("select encrypted_payload from journal_entries where id=?", byte[].class, created.id());
        assertFalse(new String(stored, java.nio.charset.StandardCharsets.UTF_8).contains("private title"));
        assertEquals(0, jdbc.queryForObject("select count(*) from outbox_events where aggregate_id=? and event_type='JournalEntrySubmitted'", Integer.class, created.id()));
        assertEquals(1, jdbc.queryForObject("select count(*) from safety_events where journal_entry_id=? and decision='SAFETY_FLOW'", Integer.class, created.id()));
        assertThrows(ResourceNotFoundException.class, () -> journal.get(stranger, created.id()));
        assertTrue(journal.list(stranger, null, null, null, null, null, 20).entries().isEmpty());
        assertEquals(LocalDate.of(2026, 1, 2), created.localDate());

        var safe = mapper.readTree("""
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"A quiet day"}]}]}
                """);
        var updated = journal.update(owner, created.id(), new UpdateJournalEntryCommand(
                null, safe, null, null, null, null, null, null, null, null), created.version());
        assertEquals(1, jdbc.queryForObject("select count(*) from safety_events where journal_entry_id=? and decision='FAIL_SAFE'", Integer.class, created.id()));
        assertEquals(2, updated.contentVersion());
        journal.favorite(owner, created.id(), true);
        assertTrue(journal.get(owner, created.id()).favorite());
        journal.delete(owner, created.id(), journal.get(owner, created.id()).version());
        assertThrows(ResourceNotFoundException.class, () -> journal.get(owner, created.id()));
        assertEquals(1, transactions.execute(status -> journalStore.purgeDeletedBefore(
                Instant.now().plusSeconds(31L * 86400))).intValue());
        assertEquals(0, jdbc.queryForObject("select count(*) from journal_entries where id=?", Integer.class, created.id()));
    }

    @Test void checkinUpsertsPerOwnerAndDateWithEncryptedNote() {
        UUID owner = user();
        UUID stranger = user();
        LocalDate date = LocalDate.of(2026, 1, 3);
        var first = new PutCheckinCommand("Asia/Ho_Chi_Minh", "calm", BigDecimal.valueOf(7),
                null, null, 420, "A private checkin note", List.of(
                new PutCheckinCommand.ActivityCommand("walking", 30, "LOW")));
        var saved = checkins.put(owner, date, first);
        assertEquals(1, saved.activities().size());
        // A client that does not send activities (null) must not wipe the ones already saved.
        var keptActivities = checkins.put(owner, date, new PutCheckinCommand("Asia/Ho_Chi_Minh", "calm",
                BigDecimal.valueOf(7), null, null, 420, "A private checkin note", null));
        assertEquals(1, keptActivities.activities().size());
        assertEquals("walking", keptActivities.activities().getFirst().code());
        var changed = checkins.put(owner, date, new PutCheckinCommand("Asia/Ho_Chi_Minh", "happy",
                BigDecimal.valueOf(8), null, null, 480, "New note", List.of()));
        assertEquals(saved.id(), changed.id());
        assertEquals(1, jdbc.queryForObject("select count(*) from daily_checkins where user_id=? and local_date=?", Integer.class, owner, date));
        assertEquals(0, changed.activities().size());
        assertEquals("New note", checkins.get(owner, date).note());
        byte[] stored = jdbc.queryForObject("select encrypted_note from daily_checkins where id=?", byte[].class, changed.id());
        assertFalse(new String(stored, java.nio.charset.StandardCharsets.UTF_8).contains("New note"));
        assertThrows(ResourceNotFoundException.class, () -> checkins.get(stranger, date));
        assertEquals(1, checkins.list(owner, date, date).size());
        assertEquals(0, checkins.list(stranger, date, date).size());
    }

    @Test void tagsAreEncryptedAndEveryHttpResourceIsOwnerScoped() throws Exception {
        UUID owner = user();
        UUID stranger = user();
        var content = mapper.readTree("""
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"Quiet morning"}]}]}
                """);
        var entry = journal.create(owner, new CreateJournalEntryCommand("Secret", content, null,
                Instant.parse("2026-01-02T12:00:00Z"), "Asia/Ho_Chi_Minh", null,
                null, null, null, null), "journal-test-key-456");
        var tag = tags.create(owner, "Private tag", "#00FF00");
        assertEquals(tag.id(), tags.create(owner, "private tag", null).id());
        tags.attach(owner, entry.id(), tag.id());
        assertEquals(1, journal.list(owner, null, null, tag.id(), null, null, 20).entries().size());
        byte[] stored = jdbc.queryForObject("select encrypted_name from journal_tags where id=?", byte[].class, tag.id());
        assertFalse(new String(stored, java.nio.charset.StandardCharsets.UTF_8).contains("Private tag"));
        assertTrue(tags.list(stranger).isEmpty());
        assertThrows(ResourceNotFoundException.class, () -> tags.attach(stranger, entry.id(), tag.id()));

        mvc.perform(get("/api/v1/journal-entries/{id}", entry.id()))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/journal-entries/{id}", entry.id())
                .with(jwt().jwt(jwt -> jwt.subject(stranger.toString()))))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/journal-entries/{id}", entry.id())
                .with(jwt().jwt(jwt -> jwt.subject(owner.toString()))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/journal-entries")
                .with(jwt().jwt(jwt -> jwt.subject(stranger.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(patch("/api/v1/journal-entries/{id}", entry.id())
                .with(jwt().jwt(jwt -> jwt.subject(stranger.toString())))
                .header("If-Match", "\"0\"").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/journal-entries/{id}", entry.id())
                .with(jwt().jwt(jwt -> jwt.subject(stranger.toString())))
                .header("If-Match", "\"0\""))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/journal-entries/{id}/favorite", entry.id())
                .with(jwt().jwt(jwt -> jwt.subject(stranger.toString()))))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/journal-entries/{id}/favorite", entry.id())
                .with(jwt().jwt(jwt -> jwt.subject(stranger.toString()))))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/journal-entries/{id}/tags/{tag}", entry.id(), tag.id())
                .with(jwt().jwt(jwt -> jwt.subject(stranger.toString()))))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/journal-entries/{id}/tags/{tag}", entry.id(), tag.id())
                .with(jwt().jwt(jwt -> jwt.subject(stranger.toString()))))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/check-ins/2026-01-03")
                .with(jwt().jwt(jwt -> jwt.subject(stranger.toString()))))
                .andExpect(status().isNotFound());
    }

    @Test void draftSuggestionRequiresAuthenticationAndValidInput() throws Exception {
        UUID owner = user();
        mvc.perform(post("/api/v1/journal-writing-suggestions")
                .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"A synthetic draft\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/journal-writing-suggestions")
                .with(jwt().jwt(jwt -> jwt.subject(owner.toString())))
                .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/journal-writing-suggestions")
                .with(jwt().jwt(jwt -> jwt.subject(owner.toString())))
                .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"A synthetic draft\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UNAVAILABLE"));
    }

    @Test void companionTitleSuggestionsAreStaticPerMoodAndNeedAuthentication() throws Exception {
        UUID owner = user();
        mvc.perform(get("/api/v1/companion/title-suggestions").param("mood", "sad"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/companion/title-suggestions").param("mood", "sad")
                .with(jwt().jwt(jwt -> jwt.subject(owner.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titles.length()").value(3))
                .andExpect(jsonPath("$.titles[0]").value("Dịu dàng với bản thân trong một ngày trùng xuống"));
        mvc.perform(get("/api/v1/companion/title-suggestions")
                .with(jwt().jwt(jwt -> jwt.subject(owner.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titles[0]").value("Vài dòng nhìn lại nhịp sống hôm nay"));
        mvc.perform(get("/api/v1/companion/title-suggestions").param("mood", "<script>")
                .with(jwt().jwt(jwt -> jwt.subject(owner.toString()))))
                .andExpect(status().isBadRequest());
    }

    @Test void onlyVerifiedResourcesAndActiveApprovedPoliciesAreUsable() throws Exception {
        UUID approver = user();
        UUID resource = UUID.randomUUID();
        Instant now = Instant.now();
        String provider = "synthetic-" + UUID.randomUUID().toString().substring(0, 8);
        String version = "test-" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("""
                insert into safety_resources(id,locale,country_code,resource_type,name,source_url,status,version,created_at,updated_at)
                values (?,'vi-VN','VN','SUPPORT','Synthetic resource','https://example.test/source','DRAFT',1,?,?)
                """, resource, java.sql.Timestamp.from(now), java.sql.Timestamp.from(now));
        assertTrue(safetyResources.approvedFor("vi-VN", "VN").isEmpty());
        jdbc.update("update safety_resources set status='APPROVED' where id=?", resource);
        assertTrue(safetyResources.approvedFor("vi-VN", "VN").isEmpty());
        jdbc.update("update safety_resources set verified_at=? where id=?", java.sql.Timestamp.from(now), resource);
        assertEquals(1, safetyResources.approvedFor("vi-VN", "VN").size());
        assertTrue(safetyResources.approvedFor("en-US", "US").isEmpty());
        mvc.perform(get("/api/v1/safety/resources?locale=vi-VN&country=VN"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/safety/resources?locale=vi-VN&country=VN")
                .with(jwt().jwt(jwt -> jwt.subject(approver.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(resource.toString()));

        String config = """
                {"allowOrdinaryAnalysis":true,"ruleVersion":"m2-curated-draft-v1",
                 "classifierProvider":"%s","classifierVersion":"v1","minConfidence":0.9}
                """.formatted(provider);
        UUID policy = UUID.randomUUID();
        jdbc.update("""
                insert into safety_policy_versions(id,version,status,config,created_at)
                values (?,?,'DRAFT',CAST(? AS jsonb),?)
                """, policy, version, config, java.sql.Timestamp.from(now));
        assertTrue(policyGate.approvedPolicy("m2-curated-draft-v1", provider, "v1",
                new BigDecimal("0.95"), now).isEmpty());
        jdbc.update("""
                update safety_policy_versions set status='APPROVED', approved_by=?,approved_at=?,effective_from=?
                where id=?
                """, approver, java.sql.Timestamp.from(now.minusSeconds(1)),
                java.sql.Timestamp.from(now.minusSeconds(1)), policy);
        assertEquals(version, policyGate.approvedPolicy("m2-curated-draft-v1", provider, "v1",
                new BigDecimal("0.95"), now).orElseThrow());
        assertTrue(policyGate.approvedPolicy("m2-curated-draft-v1", provider, "v1",
                new BigDecimal("0.80"), now).isEmpty());
        assertTrue(policyGate.approvedPolicy("m2-curated-draft-v1", "wrong-provider", "v1",
                new BigDecimal("0.95"), now).isEmpty());
    }

    @Test void journalEntriesIncludeTagsInResponseAndSupportBatchLoadingAndReplacing() throws Exception {
        UUID owner = user();
        UUID stranger = user();
        var tag1 = tags.create(owner, "Reflection", "#123456");
        var tag2 = tags.create(owner, "Mindfulness", "#654321");
        var strangerTag = tags.create(stranger, "Stranger Tag", "#000000");

        var content = mapper.readTree("""
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"A tagged entry"}]}]}
                """);
        var command = new CreateJournalEntryCommand("Tagged Title", content, null,
                Instant.parse("2026-01-02T12:00:00Z"), "Asia/Ho_Chi_Minh", "happy",
                BigDecimal.valueOf(8), null, null, null, List.of(tag1.id(), tag2.id()));

        // Create with tags
        var created = journal.create(owner, command, "journal-test-tags-1");
        assertNotNull(created.tags());
        assertEquals(2, created.tags().size());
        assertTrue(created.tags().stream().anyMatch(t -> "Reflection".equals(t.name())));
        assertTrue(created.tags().stream().anyMatch(t -> "Mindfulness".equals(t.name())));

        // Direct get
        var retrieved = journal.get(owner, created.id());
        assertNotNull(retrieved.tags());
        assertEquals(2, retrieved.tags().size());

        // Batch list
        var list = journal.list(owner, null, null, null, null, null, 20);
        var summary = list.entries().stream().filter(e -> e.id().equals(created.id())).findFirst().orElseThrow();
        assertNotNull(summary.tags());
        assertEquals(2, summary.tags().size());

        // HTTP GET /{id}
        mvc.perform(get("/api/v1/journal-entries/{id}", created.id())
                .with(jwt().jwt(jwt -> jwt.subject(owner.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags.length()").value(2))
                .andExpect(jsonPath("$.tags[0].name").isString());

        // HTTP GET / (list)
        mvc.perform(get("/api/v1/journal-entries")
                .with(jwt().jwt(jwt -> jwt.subject(owner.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].tags.length()").value(2));

        // Update tags: replace with only tag1
        var updateCommand = new UpdateJournalEntryCommand("Updated Tagged Title", content, null,
                null, null, "sad", BigDecimal.valueOf(4), null, null, null, List.of(tag1.id()));
        var updated = journal.update(owner, created.id(), updateCommand, created.version());
        assertNotNull(updated.tags());
        assertEquals(1, updated.tags().size());
        assertEquals("Reflection", updated.tags().get(0).name());

        // IDOR check: Attempting to use a tag belonging to stranger throws ResourceNotFoundException
        var idorCommand = new UpdateJournalEntryCommand(null, null, null,
                null, null, null, null, null, null, null, List.of(strangerTag.id()));
        assertThrows(ResourceNotFoundException.class, () ->
                journal.update(owner, created.id(), idorCommand, updated.version()));
    }

    @Test void searchByTitlePaginatesAndMatchesDiacritics() throws Exception {
        UUID owner = user();
        UUID stranger = user();
        var content = mapper.readTree("""
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"A private entry"}]}]}
                """);

        var cmd1 = new CreateJournalEntryCommand("Chuyến đi Đà Lạt ngày 1", content, null,
                Instant.parse("2026-01-03T12:00:00Z"), "Asia/Ho_Chi_Minh", "happy", null, null, null, null);
        var e1 = journal.create(owner, cmd1, "search-key-1");

        var cmd2 = new CreateJournalEntryCommand("Hôm nay vui vẻ", content, null,
                Instant.parse("2026-01-02T12:00:00Z"), "Asia/Ho_Chi_Minh", "happy", null, null, null, null);
        var e2 = journal.create(owner, cmd2, "search-key-2");

        var cmd3 = new CreateJournalEntryCommand("Đà Lạt ngày 2 mộng mơ", content, null,
                Instant.parse("2026-01-01T12:00:00Z"), "Asia/Ho_Chi_Minh", "happy", null, null, null, null);
        var e3 = journal.create(owner, cmd3, "search-key-3");

        var strangerCmd = new CreateJournalEntryCommand("Đà Lạt bí mật của người lạ", content, null,
                Instant.parse("2026-01-04T12:00:00Z"), "Asia/Ho_Chi_Minh", "happy", null, null, null, null);
        journal.create(stranger, strangerCmd, "search-key-stranger");

        var page1 = journal.searchByTitle(owner, "Đà Lạt", null, 1);
        assertEquals(1, page1.entries().size());
        assertEquals(e1.id(), page1.entries().get(0).id());
        assertNotNull(page1.nextCursor());

        var page2 = journal.searchByTitle(owner, "Đà Lạt", page1.nextCursor(), 1);
        assertEquals(1, page2.entries().size());
        assertEquals(e3.id(), page2.entries().get(0).id());
        assertNull(page2.nextCursor());

        var withoutDiacritics = journal.searchByTitle(owner, "da lat", null, 5);
        assertEquals(2, withoutDiacritics.entries().size());
        assertEquals(e1.id(), withoutDiacritics.entries().get(0).id());
        assertEquals(e3.id(), withoutDiacritics.entries().get(1).id());

        mvc.perform(get("/api/v1/journal-entries/search")
                .param("query", "da lat")
                .param("limit", "1")
                .with(jwt().jwt(jwt -> jwt.subject(owner.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Chuyến đi Đà Lạt ngày 1"))
                .andExpect(jsonPath("$.nextCursor").isString());

        mvc.perform(get("/api/v1/journal-entries/search")
                .param("query", "da lat")
                .with(jwt().jwt(jwt -> jwt.subject(stranger.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Đà Lạt bí mật của người lạ"));
    }

    private UUID user() {
        UUID id = UUID.randomUUID();
        String email = "m2-" + id + "@example.test";
        var encrypted = cipher.encrypt("users.email", id, id, email);
        Instant now = Instant.now();
        jdbc.update("""
                insert into users(id,email_lookup_hash,encrypted_email,email_iv,email_wrapped_key,email_key_version,
                password_hash,auth_provider,status,created_at,updated_at)
                values (?,?,?,?,?,?,'test','LOCAL','ACTIVE',?,?)
                """, id, cipher.lookupHash(email), encrypted.ciphertext(), encrypted.iv(),
                encrypted.wrappedKey(), encrypted.keyVersion(), java.sql.Timestamp.from(now),
                java.sql.Timestamp.from(now));
        return id;
    }
}
