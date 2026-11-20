package com.mylog.insight;

import com.mylog.TestcontainersConfiguration;
import com.mylog.identity.application.VerificationDelivery;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.web.InvalidRequestException;
import com.mylog.user.application.UserProfileUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** V22 thinking-pattern tables: constraints, cascades and access lockdown. Synthetic data only. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {"mylog.identity.enabled=true", "spring.mail.host=localhost",
        "MYLOG_MAIL_FROM=noreply@mylog.local", "logging.level.root=WARN"})
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ThinkingPatternSchemaIntegrationTest {
    static final Instant NOW = Instant.parse("2026-11-19T10:00:00Z");
    static final byte[] BYTES = {1};

    @Autowired JdbcTemplate jdbc;
    @Autowired SensitiveDataCipher cipher;
    @Autowired UserProfileUseCase users;
    @MockitoBean VerificationDelivery delivery;

    @Test void tablesAreLockedDownForDirectDatabaseApis() {
        for (String table : new String[] {"entry_pattern_extractions", "entry_pattern_signals", "pattern_recognitions",
                "pattern_recognition_evidence"})
            assertTrue(jdbc.queryForObject("select relrowsecurity from pg_class where relname=?", Boolean.class, table),
                    table + " has row level security");
    }

    @Test void extractionIsRecordedOncePerEntryVersionAndTaxonomy() {
        UUID user = user();
        UUID entry = entry(user);
        extraction(user, entry, 1, 0);
        assertThrows(DataAccessException.class, () -> extraction(user, entry, 1, 0), "same version is read once");
        extraction(user, entry, 2, 0); // an edited entry is read again
    }

    @Test void signalsCheckCodeShapeConfidenceAndOneRowPerCode() {
        UUID user = user();
        UUID entry = entry(user);
        UUID extraction = extraction(user, entry, 1, 1);
        signal(extraction, user, entry, "CATASTROPHIZING", "0.9");
        assertThrows(DataAccessException.class, () -> signal(extraction, user, entry, "CATASTROPHIZING", "0.8"));
        assertThrows(DataAccessException.class, () -> signal(extraction, user, entry, "lowercase", "0.8"));
        assertThrows(DataAccessException.class, () -> signal(extraction, user, entry, "RUMINATION", "1.2"));
    }

    @Test void onlyOneLiveCardPerCodeAndFeedbackIsConsistent() {
        UUID user = user();
        UUID first = card(user, "RUMINATION", "ACTIVE");
        assertThrows(DataAccessException.class, () -> card(user, "RUMINATION", "ACTIVE"));
        jdbc.update("update pattern_recognitions set status='SUPERSEDED', superseded_at=? where id=?", Timestamp.from(NOW), first);
        card(user, "RUMINATION", "ACTIVE");
        card(user, "RUMINATION", "HIDDEN");

        UUID other = card(user, "AVOIDANCE", "ACTIVE");
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "update pattern_recognitions set user_feedback='CONFIRMED' where id=?", other), "feedback needs a time");
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "update pattern_recognitions set user_feedback='CONFIRMED', feedback_at=?, feedback_reason='WRONG_QUOTE' where id=?",
                Timestamp.from(NOW), other), "a reason only goes with NOT_REALLY");
        jdbc.update("update pattern_recognitions set user_feedback='NOT_REALLY', feedback_at=?, feedback_reason='WRONG_QUOTE' where id=?",
                Timestamp.from(NOW), other);
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "update pattern_recognitions set generated_by='AI_ASSISTED' where id=?", other), "AI wording names its model");
    }

    @Test void deletingAnEntryRemovesItsSignalsAndTheirEvidenceButKeepsTheCard() {
        UUID user = user();
        UUID entry = entry(user);
        UUID signal = signal(extraction(user, entry, 1, 1), user, entry, "SELF_LABELING", "0.85");
        UUID card = card(user, "SELF_LABELING", "ACTIVE");
        jdbc.update("insert into pattern_recognition_evidence(recognition_id,signal_id,shown_as_quote) values (?,?,true)", card, signal);

        jdbc.update("delete from journal_entries where id=?", entry);
        assertEquals(0, count("entry_pattern_extractions where journal_entry_id=?", entry));
        assertEquals(0, count("entry_pattern_signals where id=?", signal));
        assertEquals(0, count("pattern_recognition_evidence where recognition_id=?", card));
        assertEquals(1, count("pattern_recognitions where id=?", card));
    }

    @Test void deletingThePlanGoalKeepsTheCard() {
        UUID user = user();
        UUID goal = UUID.randomUUID();
        jdbc.update("""
                insert into selfcare_goals(id,user_id,category,encrypted_title,title_iv,title_wrapped_key,title_key_version,
                    status,created_at,updated_at) values (?,?,'MINDFULNESS',?,?,?,'synthetic','ACTIVE',?,?)
                """, goal, user, BYTES, BYTES, BYTES, Timestamp.from(NOW), Timestamp.from(NOW));
        UUID card = card(user, "PERFECTIONISM", "ACTIVE");
        jdbc.update("update pattern_recognitions set plan_goal_id=?, plan_started_at=? where id=?", goal, Timestamp.from(NOW), card);
        jdbc.update("delete from selfcare_goals where id=?", goal);
        assertNull(jdbc.queryForObject("select plan_goal_id from pattern_recognitions where id=?", UUID.class, card));
    }

    @Test void deletingTheUserRemovesEverything() {
        UUID user = user();
        UUID entry = entry(user);
        UUID signal = signal(extraction(user, entry, 1, 1), user, entry, "WORRY_LOOP", "0.8");
        UUID card = card(user, "WORRY_LOOP", "ACTIVE");
        jdbc.update("insert into pattern_recognition_evidence(recognition_id,signal_id) values (?,?)", card, signal);
        jdbc.update("delete from users where id=?", user);
        for (String table : new String[] {"entry_pattern_extractions", "entry_pattern_signals", "pattern_recognitions"})
            assertEquals(0, count(table + " where user_id=?", user), table);
    }

    @Test void thinkingPatternsIsASeparateOptInConsent() {
        UUID user = user();
        assertTrue(!users.isConsentGranted(user, "THINKING_PATTERNS"), "off until the user opts in");
        users.decideConsent(user, "THINKING_PATTERNS", "v1", true);
        assertTrue(users.isConsentGranted(user, "THINKING_PATTERNS"));
        assertThrows(InvalidRequestException.class, () -> users.decideConsent(user, "SOMETHING_ELSE", "v1", true));
    }

    // ---- fixtures ------------------------------------------------------------------------------

    private UUID extraction(UUID user, UUID entry, int contentVersion, int signals) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into entry_pattern_extractions(id,user_id,journal_entry_id,content_version,taxonomy_version,
                    signal_count,extractor_provider,extractor_model,extractor_version,created_at)
                values (?,?,?,?,'tp-v1',?,'fake','keyword-rules','fake-v1',?)
                """, id, user, entry, contentVersion, signals, Timestamp.from(NOW));
        return id;
    }

    private UUID signal(UUID extraction, UUID user, UUID entry, String code, String confidence) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into entry_pattern_signals(id,extraction_id,user_id,journal_entry_id,pattern_code,confidence,
                    encrypted_quote,quote_iv,quote_wrapped_key,quote_key_version,created_at)
                values (?,?,?,?,?,?,?,?,?,'synthetic',?)
                """, id, extraction, user, entry, code, new BigDecimal(confidence), BYTES, BYTES, BYTES, Timestamp.from(NOW));
        return id;
    }

    private UUID card(UUID user, String code, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into pattern_recognitions(id,user_id,pattern_code,taxonomy_version,period_start,period_end,timezone,
                    status,entry_count,week_count,average_confidence,encrypted_narrative,narrative_iv,narrative_wrapped_key,
                    narrative_key_version,generated_by,created_at,updated_at)
                values (?,?,?,'tp-v1',?,?,'Asia/Ho_Chi_Minh',?,3,2,0.8,?,?,?,'synthetic','TEMPLATE',?,?)
                """, id, user, code, LocalDate.of(2026, 10, 21), LocalDate.of(2026, 11, 19), status,
                BYTES, BYTES, BYTES, Timestamp.from(NOW), Timestamp.from(NOW));
        return id;
    }

    private UUID entry(UUID user) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into journal_entries(id,user_id,encrypted_payload,payload_iv,wrapped_data_key,
                    encryption_key_version,content_format_version,occurred_at,local_date,timezone,
                    entry_status,risk_level,analysis_status,content_version,created_at,updated_at)
                values (?,?,?,?,?,'synthetic',1,?,?,'Asia/Ho_Chi_Minh','SAVED','NORMAL','PENDING',1,?,?)
                """, id, user, BYTES, BYTES, BYTES, Timestamp.from(NOW), LocalDate.of(2026, 11, 18),
                Timestamp.from(NOW), Timestamp.from(NOW));
        return id;
    }

    private UUID user() {
        UUID id = UUID.randomUUID();
        String email = "thinking-" + id + "@example.test";
        var encrypted = cipher.encrypt("users.email", id, id, email);
        jdbc.update("""
                insert into users(id,email_lookup_hash,encrypted_email,email_iv,email_wrapped_key,email_key_version,
                    password_hash,auth_provider,status,created_at,updated_at)
                values (?,?,?,?,?,?,'test','LOCAL','ACTIVE',?,?)
                """, id, cipher.lookupHash(email), encrypted.ciphertext(), encrypted.iv(),
                encrypted.wrappedKey(), encrypted.keyVersion(), Timestamp.from(NOW), Timestamp.from(NOW));
        users.createDefault(id, "Asia/Ho_Chi_Minh", "vi", "v1", "v1", NOW);
        return id;
    }

    private int count(String where, Object... args) {
        return jdbc.queryForObject("select count(*) from " + where, Integer.class, args);
    }
}
