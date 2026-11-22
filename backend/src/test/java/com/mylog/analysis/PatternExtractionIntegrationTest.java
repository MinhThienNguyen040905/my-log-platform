package com.mylog.analysis;

import com.mylog.TestcontainersConfiguration;
import com.mylog.analysis.application.PatternExtractionService;
import com.mylog.identity.application.VerificationDelivery;
import com.mylog.journal.application.JournalService;
import com.mylog.journal.application.command.CreateJournalEntryCommand;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.user.application.UserProfileUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Thinking-pattern extraction with the keyword extractor against PostgreSQL. Synthetic data only. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {"mylog.identity.enabled=true", "mylog.patterns.extractor=fake", "spring.mail.host=localhost",
        "MYLOG_MAIL_FROM=noreply@mylog.local", "logging.level.root=WARN"})
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PatternExtractionIntegrationTest {
    static final String PATTERN = "Lỡ dự án trễ thêm một tuần nữa thì coi như sự nghiệp tiêu tan.";
    static final String NEUTRAL = "Tối nay mình nấu canh chua và rau muống xào tỏi.";

    @Autowired PatternExtractionService extraction;
    @Autowired JournalService journal;
    @Autowired UserProfileUseCase users;
    @Autowired SensitiveDataCipher cipher;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @MockitoBean VerificationDelivery delivery;

    @Test void nothingIsReadWithoutBothConsents() throws Exception {
        UUID user = user();
        entry(user, PATTERN, "NORMAL");
        users.decideConsent(user, "AI_PROCESSING", "v1", true);
        assertEquals("CONSENT_REQUIRED", extraction.extractRecent(user).status());
        assertEquals(0, count("entry_pattern_extractions where user_id=?", user));
    }

    @Test void readsAllowedEntriesOnceAndKeepsQuotesEncrypted() throws Exception {
        UUID user = user();
        UUID withPattern = entry(user, "Sáng đi làm. " + PATTERN, "NORMAL");
        UUID neutral = entry(user, NEUTRAL, "LOW");
        UUID risky = entry(user, PATTERN, "HIGH");
        UUID unscreened = entry(user, PATTERN, "UNKNOWN");
        UUID stranger = user();
        UUID strangers = entry(stranger, PATTERN, "NORMAL");
        consent(user);

        var summary = extraction.extractRecent(user);
        assertEquals("OK", summary.status());
        assertEquals(2, summary.read());
        assertEquals(1, summary.signals());
        assertFalse(summary.more());
        assertEquals(Map.of(withPattern, 1, neutral, 0), extractions(user), "read once each, empty reads recorded");
        assertEquals(0, count("entry_pattern_extractions where journal_entry_id in (?,?,?)", risky, unscreened, strangers));

        var row = jdbc.queryForMap("select id, pattern_code, encrypted_quote, quote_iv, quote_wrapped_key, quote_key_version "
                + "from entry_pattern_signals where journal_entry_id=?", withPattern);
        assertEquals("CATASTROPHIZING", row.get("pattern_code"));
        byte[] ciphertext = (byte[]) row.get("encrypted_quote");
        assertFalse(new String(ciphertext, java.nio.charset.StandardCharsets.UTF_8).contains("dự án"), "stored encrypted");
        assertEquals(PATTERN, cipher.decrypt("entry_pattern_signals.quote", user, (UUID) row.get("id"),
                new SensitiveDataCipher.Encrypted(ciphertext, (byte[]) row.get("quote_iv"),
                        (byte[]) row.get("quote_wrapped_key"), (String) row.get("quote_key_version"))));
        assertTrue(count("ai_usage_records where operation='PATTERN_EXTRACTION' and provider='fake' and created_at>=?",
                Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS))) >= 2, "usage recorded per provider call");

        assertEquals(0, extraction.extractRecent(user).read(), "unchanged entries are not read again");
    }

    @Test void editedEntriesAreReadAgainAndDeletedOrRiskyOnesAreDropped() throws Exception {
        UUID user = user();
        UUID edited = entry(user, PATTERN, "NORMAL");
        UUID deleted = entry(user, PATTERN, "NORMAL");
        UUID escalated = entry(user, PATTERN, "NORMAL");
        consent(user);
        extraction.extractRecent(user);
        assertEquals(3, count("entry_pattern_signals where user_id=?", user));

        jdbc.update("update journal_entries set content_version=content_version+1 where id=?", edited);
        jdbc.update("update journal_entries set deleted_at=now() where id=?", deleted);
        jdbc.update("update journal_entries set risk_level='HIGH' where id=?", escalated);
        assertEquals(1, extraction.extractRecent(user).read());
        assertEquals(Map.of(edited, 1), extractions(user));
        assertEquals(2, jdbc.queryForObject("select content_version from entry_pattern_extractions where journal_entry_id=?",
                Integer.class, edited));
        assertEquals(1, count("entry_pattern_signals where user_id=?", user));
    }

    // ---- fixtures ------------------------------------------------------------------------------

    private Map<UUID, Integer> extractions(UUID user) {
        Map<UUID, Integer> out = new java.util.HashMap<>();
        jdbc.query("select journal_entry_id, signal_count from entry_pattern_extractions where user_id=?",
                rs -> { out.put(rs.getObject(1, UUID.class), rs.getInt(2)); }, user);
        return out;
    }

    private void consent(UUID user) {
        users.decideConsent(user, "AI_PROCESSING", "v1", true);
        users.decideConsent(user, "THINKING_PATTERNS", "v1", true);
    }

    /** Creates an entry through the journal service, then sets the screening outcome the test needs. */
    private UUID entry(UUID user, String text, String risk) throws Exception {
        var content = mapper.readTree(mapper.writeValueAsString(Map.of("type", "doc", "content", List.of(
                Map.of("type", "paragraph", "content", List.of(Map.of("type", "text", "text", text)))))));
        UUID id = journal.create(user, new CreateJournalEntryCommand("Nhật ký thử", content, null,
                Instant.now().minus(1, ChronoUnit.DAYS), "Asia/Ho_Chi_Minh", null, null, null, null, null),
                "pattern-" + UUID.randomUUID()).id();
        jdbc.update("update journal_entries set risk_level=? where id=?", risk, id);
        return id;
    }

    private UUID user() {
        UUID id = UUID.randomUUID();
        String email = "extract-" + id + "@example.test";
        var encrypted = cipher.encrypt("users.email", id, id, email);
        jdbc.update("""
                insert into users(id,email_lookup_hash,encrypted_email,email_iv,email_wrapped_key,email_key_version,
                    password_hash,auth_provider,status,created_at,updated_at)
                values (?,?,?,?,?,?,'test','LOCAL','ACTIVE',now(),now())
                """, id, cipher.lookupHash(email), encrypted.ciphertext(), encrypted.iv(),
                encrypted.wrappedKey(), encrypted.keyVersion());
        // Default refusals a minute in the past so the test's own consent decisions are the latest.
        users.createDefault(id, "Asia/Ho_Chi_Minh", "vi", "v1", "v1", Instant.now().minusSeconds(60));
        return id;
    }

    private int count(String where, Object... args) {
        return jdbc.queryForObject("select count(*) from " + where, Integer.class, args);
    }
}
