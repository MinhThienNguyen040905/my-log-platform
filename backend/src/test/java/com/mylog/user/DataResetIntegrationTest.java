package com.mylog.user;

import com.mylog.TestcontainersConfiguration;
import com.mylog.feedback.application.FeedbackService;
import com.mylog.identity.application.VerificationDelivery;
import com.mylog.journal.application.JournalService;
import com.mylog.journal.application.command.CreateJournalEntryCommand;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.user.application.UserProfileUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** "Start a new journey": POST /api/v1/me/data:reset deletes the user's data and keeps the account. Synthetic data only. */
@Import({TestcontainersConfiguration.class, DataResetIntegrationTest.FixedClock.class})
@SpringBootTest(properties = {"mylog.identity.enabled=true", "spring.mail.host=localhost",
        "MYLOG_MAIL_FROM=noreply@mylog.local", "logging.level.root=WARN"})
@ActiveProfiles("test") @AutoConfigureMockMvc @Testcontainers(disabledWithoutDocker = true)
class DataResetIntegrationTest {
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    static class FixedClock {
        @Bean @Primary Clock clockDataReset() { return Clock.fixed(NOW, ZoneId.of("UTC")); }
    }
    private static final Instant NOW = Instant.parse("2026-10-10T03:00:00Z");
    private static final String PASSWORD = "CorrectHorseBattery7!";
    @Autowired JdbcTemplate jdbc;
    @Autowired SensitiveDataCipher cipher;
    @Autowired UserProfileUseCase profiles;
    @Autowired FeedbackService feedback;
    @Autowired JournalService journal;
    @Autowired ObjectMapper mapper;
    @Autowired MockMvc mvc;
    @MockitoBean VerificationDelivery delivery;

    @Test void resetDeletesOwnDataKeepsAccountAndLeavesOthersAlone() throws Exception {
        UUID owner = user(true), other = user(true);
        UUID entry = seed(owner);
        UUID otherEntry = seed(other);
        UUID session = session(owner, NOW);

        mvc.perform(reset(owner, session, "WrongPassword123!"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail").value("PASSWORD_INCORRECT"));
        assertEquals(1, count("journal_entries where id=?", entry), "a wrong password deletes nothing");

        mvc.perform(reset(owner, session, PASSWORD)).andExpect(status().isNoContent());
        for (String table : new String[] {"journal_entries", "journal_tags", "daily_checkins", "feedback", "safety_events",
                "ai_jobs", "insights", "reports", "idempotency_keys"})
            assertEquals(0, count(table + " where user_id=?", owner), table + " is emptied");
        assertEquals(0, count("outbox_events where aggregate_id=?", entry));
        assertEquals("ACTIVE", jdbc.queryForObject("select status from users where id=?", String.class, owner));
        assertEquals(1, count("user_profiles where user_id=?", owner), "profile kept");
        assertEquals(1, count("auth_sessions where id=?", session), "sign-in kept");
        assertEquals(5, count("user_consents where user_id=?", owner), "consents kept");
        assertEquals(1, count("audit_logs where action='DATA_RESET' and target_id=?", owner));

        assertEquals(1, count("journal_entries where id=?", otherEntry), "another user's data is untouched");
        assertEquals(1, count("daily_checkins where user_id=?", other));
        assertEquals(1, count("feedback where user_id=?", other));

        // Resetting again is harmless.
        mvc.perform(reset(owner, session, PASSWORD)).andExpect(status().isNoContent());
    }

    @Test void wrongPasswordsAreRateLimited() throws Exception {
        UUID owner = user(true);
        UUID session = session(owner, NOW);
        for (int i = 0; i < 5; i++) mvc.perform(reset(owner, session, "WrongPassword123!")).andExpect(status().isForbidden());
        mvc.perform(reset(owner, session, PASSWORD)).andExpect(status().isTooManyRequests());
    }

    @Test void passwordlessAccountsNeedARecentSignIn() throws Exception {
        UUID owner = user(false);
        UUID entry = seed(owner);
        UUID stale = session(owner, NOW.minusSeconds(3600));
        mvc.perform(reset(owner, stale, null))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail").value("RECENT_LOGIN_REQUIRED"));
        assertEquals(1, count("journal_entries where id=?", entry));
        UUID fresh = session(owner, NOW.minusSeconds(120));
        mvc.perform(reset(owner, fresh, null)).andExpect(status().isNoContent());
        assertEquals(0, count("journal_entries where user_id=?", owner));
    }

    @Test void confirmationIsRequired() throws Exception {
        UUID owner = user(true);
        UUID session = session(owner, NOW);
        mvc.perform(post("/api/v1/me/data:reset").with(jwt().jwt(j -> j.subject(owner.toString()).claim("sid", session.toString())))
                        .contentType("application/json").content("{\"password\":\"" + PASSWORD + "\",\"confirm\":false}"))
                .andExpect(status().isBadRequest());
    }

    private MockHttpServletRequestBuilder reset(UUID user, UUID session, String password) {
        String body = password == null ? "{\"confirm\":true}" : "{\"password\":\"" + password + "\",\"confirm\":true}";
        return post("/api/v1/me/data:reset").with(jwt().jwt(j -> j.subject(user.toString()).claim("sid", session.toString())))
                .contentType("application/json").content(body);
    }

    /** One journal entry with a tag, a check-in, a feedback item and a safety event for {@code user}. */
    private UUID seed(UUID user) throws Exception {
        UUID entry = journal.create(user, new CreateJournalEntryCommand("Synthetic diary",
                        mapper.readTree("{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"A calm day\"}]}]}"),
                        null, NOW, "Asia/Ho_Chi_Minh", null, null, null, null, null),
                "reset-entry-" + UUID.randomUUID()).id();
        var tag = cipher.encrypt("journal_tags.name", user, user, "synthetic-tag");
        jdbc.update("insert into journal_tags(id,user_id,name_lookup_hash,encrypted_name,name_iv,name_wrapped_key,name_key_version,created_at,updated_at)"
                + " values (?,?,?,?,?,?,?,?,?)", UUID.randomUUID(), user, cipher.lookupHash("tag-" + user), tag.ciphertext(), tag.iv(),
                tag.wrappedKey(), tag.keyVersion(), Timestamp.from(NOW), Timestamp.from(NOW));
        mvc.perform(put("/api/v1/check-ins/2026-10-10").with(jwt().jwt(j -> j.subject(user.toString())))
                        .contentType("application/json").content("{\"timezone\":\"Asia/Ho_Chi_Minh\",\"moodCode\":\"happy\",\"moodScore\":8}"))
                .andExpect(status().isOk());
        feedback.submit(user, "IDEA", "Synthetic idea");
        jdbc.update("insert into safety_events(id,user_id,subject_ref,journal_entry_id,risk_level,decision,policy_version,created_at)"
                + " values (?,?,?,?,'LOW','ALLOW','test',?)", UUID.randomUUID(), user, new byte[] {1}, entry, Timestamp.from(NOW));
        return entry;
    }

    private UUID session(UUID user, Instant createdAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into auth_sessions(id,user_id,token_family_id,current_token_hash,last_used_at,expires_at,created_at)
                values (?,?,?,?,?,?,?)
                """, id, user, UUID.randomUUID(), cipher.tokenHash("reset-session-" + id),
                Timestamp.from(createdAt), Timestamp.from(NOW.plusSeconds(3600)), Timestamp.from(createdAt));
        return id;
    }

    private UUID user(boolean withPassword) {
        UUID id = UUID.randomUUID();
        String email = "reset-" + id + "@example.test";
        var encrypted = cipher.encrypt("users.email", id, id, email);
        jdbc.update("""
                insert into users(id,email_lookup_hash,encrypted_email,email_iv,email_wrapped_key,email_key_version,
                    password_hash,auth_provider,status,created_at,updated_at)
                values (?,?,?,?,?,?,?,?,'ACTIVE',?,?)
                """, id, cipher.lookupHash(email), encrypted.ciphertext(), encrypted.iv(), encrypted.wrappedKey(),
                encrypted.keyVersion(), withPassword ? new BCryptPasswordEncoder(12).encode(PASSWORD) : null,
                withPassword ? "LOCAL" : "GOOGLE", Timestamp.from(NOW), Timestamp.from(NOW));
        profiles.createDefault(id, "Asia/Ho_Chi_Minh", "vi", "v1", "v1", NOW);
        return id;
    }

    private int count(String from, UUID id) {
        Integer value = jdbc.queryForObject("select count(*) from " + from, Integer.class, id);
        return value == null ? 0 : value;
    }
}
