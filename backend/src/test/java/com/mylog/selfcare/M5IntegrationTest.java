package com.mylog.selfcare;

import com.mylog.TestcontainersConfiguration;
import com.mylog.identity.application.VerificationDelivery;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.web.ConflictException;
import com.mylog.platform.web.InvalidRequestException;
import com.mylog.platform.web.ResourceNotFoundException;
import com.mylog.selfcare.application.SelfCareService;
import com.mylog.user.application.UserProfileUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({TestcontainersConfiguration.class, M5IntegrationTest.FixedClock.class})
@SpringBootTest(properties = {"mylog.identity.enabled=true", "spring.mail.host=localhost",
        "MYLOG_MAIL_FROM=noreply@mylog.local", "logging.level.root=WARN"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class M5IntegrationTest {
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    static class FixedClock {
        @Bean @Primary Clock m5Clock() { return Clock.fixed(Instant.parse("2026-10-04T18:00:00Z"), ZoneId.of("UTC")); }
    }
    @Autowired SelfCareService service;
    @Autowired UserProfileUseCase users;
    @Autowired SensitiveDataCipher cipher;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @MockitoBean VerificationDelivery delivery;

    @Test void encryptedOwnerScopedOptimisticAndIdempotentCompletion() throws Exception {
        UUID owner = user(); UUID stranger = user();
        assertThrows(InvalidRequestException.class,
                () -> service.createGoal(owner, "SLEEP", "Điều trị mất ngủ", null, null, null));
        var goal = service.createGoal(owner, "MINDFULNESS", "Thở chậm mỗi sáng", "Tự chăm sóc bản thân",
                LocalDate.of(2026, 10, 5), null);
        assertEquals("Thở chậm mỗi sáng", goal.title());
        byte[] rawTitle = jdbc.queryForObject("select encrypted_title from selfcare_goals where id=?", byte[].class, goal.id());
        assertFalse(new String(rawTitle, StandardCharsets.UTF_8).contains("Thở chậm"));
        assertThrows(ResourceNotFoundException.class,
                () -> service.updateGoal(stranger, goal.id(), "X", null, null, null, null, 0));
        var habit = service.createHabit(owner, goal.id(), "Thiền 5 phút", BigDecimal.valueOf(5),
                "minutes", "DAILY", null);
        assertEquals("Asia/Ho_Chi_Minh", habit.timezone());
        assertThrows(ResourceNotFoundException.class,
                () -> service.putCompletion(stranger, habit.id(), LocalDate.of(2026, 10, 5), BigDecimal.valueOf(5)));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update("""
                insert into habit_completions(id,habit_id,user_id,local_date,value,source,created_at,updated_at)
                values (?,?,?,?,5,'USER',?,?)
                """, UUID.randomUUID(), habit.id(), stranger, LocalDate.of(2026, 10, 5),
                Timestamp.from(Instant.parse("2026-10-04T18:00:00Z")),
                Timestamp.from(Instant.parse("2026-10-04T18:00:00Z"))));
        assertThrows(InvalidRequestException.class,
                () -> service.putCompletion(owner, habit.id(), LocalDate.of(2026, 10, 6), BigDecimal.valueOf(5)));
        for (int i = 0; i < 2; i++) service.putCompletion(owner, habit.id(), LocalDate.of(2026, 10, 5), BigDecimal.valueOf(5));
        assertEquals(1, jdbc.queryForObject("select count(*) from habit_completions where habit_id=?", Integer.class, habit.id()));
        assertEquals(1, service.goals(owner).getFirst().habits().getFirst().streak());
        assertEquals(1, service.goals(owner).getFirst().habits().getFirst().completionCount());
        var updated = service.updateGoal(owner, goal.id(), "Ngồi yên mỗi sáng", null, null, null, null, 0);
        assertEquals(1, updated.version());
        assertThrows(ConflictException.class,
                () -> service.updateGoal(owner, goal.id(), "Stale", null, null, null, null, 0));
        mvc.perform(get("/api/v1/self-care/goals")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/self-care/goals").with(jwt().jwt(j -> j.subject(owner.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].habits[0].streak").value(1));
        mvc.perform(patch("/api/v1/self-care/goals/{id}", goal.id())
                .with(jwt().jwt(j -> j.subject(stranger.toString())))
                .header("If-Match", "\"1\"").contentType("application/json").content("{\"status\":\"PAUSED\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/self-care/goals").with(jwt().jwt(j -> j.subject(owner.toString())))
                .contentType("application/json")
                .content("{\"category\":\"SLEEP\",\"title\":\"Ngủ đúng giờ\"}"))
                .andExpect(status().isCreated()).andExpect(header().string("ETag", "\"0\""));
        mvc.perform(post("/api/v1/self-care/goals/{id}/habits", goal.id())
                .with(jwt().jwt(j -> j.subject(stranger.toString())))
                .contentType("application/json")
                .content("{\"title\":\"Đi bộ\",\"targetValue\":1,\"unit\":\"minutes\",\"frequencyType\":\"DAILY\"}"))
                .andExpect(status().isNotFound());
        service.deleteCompletion(owner, habit.id(), LocalDate.of(2026, 10, 5));
        service.deleteCompletion(owner, habit.id(), LocalDate.of(2026, 10, 5));
        assertEquals(0, service.goals(owner).stream().filter(g -> g.id().equals(goal.id()))
                .findFirst().orElseThrow().habits().getFirst().completionCount());
    }

    @Test void habitUpdateAndDeleteAreOwnerScopedVersionedAndCascade() throws Exception {
        UUID owner = user(); UUID stranger = user();
        var goal = service.createGoal(owner, "EXERCISE", "Vận động nhẹ", null, null, null);
        var habit = service.createHabit(owner, goal.id(), "Đi bộ", BigDecimal.valueOf(10), "minutes", "DAILY", null);
        service.putCompletion(owner, habit.id(), LocalDate.of(2026, 10, 5), BigDecimal.valueOf(10));

        assertThrows(ResourceNotFoundException.class, () -> service.updateHabit(stranger, habit.id(), "X",
                null, null, null, null, null, null, 0));
        assertThrows(InvalidRequestException.class, () -> service.updateHabit(owner, habit.id(), null,
                null, null, "WEEKLY", null, null, null, 0));
        assertThrows(InvalidRequestException.class, () -> service.updateHabit(owner, habit.id(), null,
                null, null, null, null, null, "COMPLETED", 0));
        var updated = service.updateHabit(owner, habit.id(), "Đi bộ buổi tối", BigDecimal.valueOf(15), null,
                "WEEKLY", List.of(1, 3, 5), "BREATHING_2MIN", "PAUSED", 0);
        assertEquals("Đi bộ buổi tối", updated.title());
        assertEquals(1, updated.version());
        assertEquals(List.of(1, 3, 5), updated.daysOfWeek());
        assertEquals("BREATHING_2MIN", updated.toolCode());
        assertEquals("PAUSED", updated.status());
        byte[] rawTitle = jdbc.queryForObject("select encrypted_title from habits where id=?", byte[].class, habit.id());
        assertFalse(new String(rawTitle, StandardCharsets.UTF_8).contains("Đi bộ"));
        assertThrows(ConflictException.class, () -> service.updateHabit(owner, habit.id(), "Stale",
                null, null, null, null, null, null, 0));
        var cleared = service.updateHabit(owner, habit.id(), null, null, null, null, null, "", "ACTIVE", 1);
        assertNull(cleared.toolCode());
        assertEquals(List.of(1, 3, 5), cleared.daysOfWeek());

        mvc.perform(patch("/api/v1/self-care/habits/{id}", habit.id())
                        .with(jwt().jwt(j -> j.subject(owner.toString())))
                        .header("If-Match", "\"2\"").contentType("application/json")
                        .content("{\"frequencyType\":\"DAILY\",\"daysOfWeek\":[]}"))
                .andExpect(status().isOk()).andExpect(header().string("ETag", "\"3\""))
                .andExpect(jsonPath("$.frequencyType").value("DAILY"));
        mvc.perform(delete("/api/v1/self-care/habits/{id}", habit.id())
                        .with(jwt().jwt(j -> j.subject(stranger.toString()))))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/self-care/habits/{id}", habit.id())
                        .with(jwt().jwt(j -> j.subject(owner.toString()))))
                .andExpect(status().isNoContent());
        assertEquals(0, jdbc.queryForObject("select count(*) from habit_completions where habit_id=?",
                Integer.class, habit.id()));
        mvc.perform(delete("/api/v1/self-care/habits/{id}", habit.id())
                        .with(jwt().jwt(j -> j.subject(owner.toString()))))
                .andExpect(status().isNotFound());

        var second = service.createHabit(owner, goal.id(), "Giãn cơ", BigDecimal.ONE, "sessions", "DAILY", null);
        service.putCompletion(owner, second.id(), LocalDate.of(2026, 10, 5), BigDecimal.ONE);
        assertThrows(ResourceNotFoundException.class, () -> service.deleteGoal(stranger, goal.id()));
        mvc.perform(delete("/api/v1/self-care/goals/{id}", goal.id()))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/self-care/goals/{id}", goal.id())
                        .with(jwt().jwt(j -> j.subject(owner.toString()))))
                .andExpect(status().isNoContent());
        assertEquals(0, jdbc.queryForObject("select count(*) from habits where goal_id=?", Integer.class, goal.id()));
        assertEquals(0, jdbc.queryForObject("select count(*) from habit_completions where habit_id=?",
                Integer.class, second.id()));
        assertTrue(service.goals(owner).isEmpty());
    }

    private UUID user() {
        UUID id = UUID.randomUUID(); String email = "m5-" + id + "@example.test";
        var encrypted = cipher.encrypt("users.email", id, id, email);
        Instant now = Instant.parse("2026-10-04T18:00:00Z");
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
