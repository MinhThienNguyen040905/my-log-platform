package com.mylog.knowledge;

import com.mylog.TestcontainersConfiguration;
import com.mylog.analysis.application.KnowledgeRetriever;
import com.mylog.identity.application.IdentityStore;
import com.mylog.identity.application.VerificationDelivery;
import com.mylog.journal.application.JournalService;
import com.mylog.journal.application.command.CreateJournalEntryCommand;
import com.mylog.knowledge.application.KnowledgeService;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({TestcontainersConfiguration.class,M6IntegrationTest.FixedClock.class})
@SpringBootTest(properties={"mylog.identity.enabled=true","spring.mail.host=localhost",
        "MYLOG_MAIL_FROM=noreply@mylog.local","logging.level.root=WARN"})
@ActiveProfiles("test") @AutoConfigureMockMvc @Testcontainers(disabledWithoutDocker=true)
class M6IntegrationTest {
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods=false)
    static class FixedClock {
        @Bean @Primary Clock clockM6() { return Clock.fixed(Instant.parse("2026-10-10T00:00:00Z"),ZoneId.of("UTC")); }
    }
    @Autowired KnowledgeService knowledge;
    @Autowired KnowledgeRetriever retriever;
    @Autowired IdentityStore identity;
    @Autowired UserProfileUseCase profiles;
    @Autowired SensitiveDataCipher cipher;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired JournalService journal;
    @Autowired ObjectMapper mapper;
    @MockitoBean VerificationDelivery delivery;
    private static final Instant NOW=Instant.parse("2026-10-10T00:00:00Z");

    @Test void workflowRetrievalCitationAndAdminPermissionMatrix() throws Exception {
        UUID editor=user(); UUID reviewer=user(); UUID ordinary=user();
        role(editor,"CONTENT_EDITOR"); role(reviewer,"CONTENT_APPROVER");
        assertTrue(identity.permissions(editor).contains("knowledge:write"));
        assertFalse(identity.permissions(editor).contains("knowledge:review"));
        assertTrue(identity.permissions(reviewer).contains("knowledge:review"));
        var draft=knowledge.create(editor,"mindful-breathing","MINDFULNESS","vi","Reviewed guide",
                "https://example.org/mindfulness","content-team","Bài tập thở",
                "Hít thở chậm trong vài phút.\n\nTạm nghỉ khi cần.",NOW.minusSeconds(60),null);
        assertTrue(retriever.retrieve("MINDFULNESS","vi",3).isEmpty());
        var unchanged=knowledge.updateDraft(editor,draft.itemId(),1,"Bài tập thở",
                "Hít thở chậm trong vài phút.\n\nTạm nghỉ khi cần.");
        assertEquals(draft.checksum(),unchanged.checksum());
        knowledge.submit(editor,draft.itemId(),1);
        assertTrue(retriever.retrieve("MINDFULNESS","vi",3).isEmpty());
        mvc.perform(post("/api/v1/admin/knowledge-items/{id}/versions/1:approve",draft.itemId())
                .with(jwt().jwt(j -> j.subject(editor.toString()))
                        .authorities(new SimpleGrantedAuthority("knowledge:write"))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/knowledge-items/{id}/versions/1:approve",draft.itemId())
                .with(jwt().jwt(j -> j.subject(reviewer.toString()))
                        .authorities(new SimpleGrantedAuthority("knowledge:review"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        var passages=retriever.retrieve("MINDFULNESS","vi",3);
        assertEquals(2,passages.size());
        assertEquals(draft.versionId(),passages.getFirst().versionId());
        assertTrue(retriever.retrieve("MINDFULNESS","en",3).isEmpty());
        var future=knowledge.create(editor,"future-mindfulness","MINDFULNESS","vi","Future guide",
                "https://example.org/future","content-team","Bản tương lai","Nội dung tương lai.",
                NOW.plusSeconds(86400),null);
        knowledge.submit(editor,future.itemId(),1);
        knowledge.review(reviewer,future.itemId(),1,true,null);
        assertEquals(2,retriever.retrieve("MINDFULNESS","vi",3).size());
        mvc.perform(get("/api/v1/recommendations?topicCode=MINDFULNESS")
                .with(jwt().jwt(j -> j.subject(ordinary.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.excerpts[0].citation.versionId").value(draft.versionId().toString()));
        assertThrows(org.springframework.dao.DataAccessException.class,() -> jdbc.update(
                "update knowledge_versions set content='tampered' where id=?",draft.versionId()));
        assertThrows(org.springframework.dao.DataAccessException.class,() -> jdbc.update("""
                insert into knowledge_chunks(id,knowledge_version_id,chunk_index,content,token_count,created_at)
                values (?,?,99,'unreviewed',1,?)
                """,UUID.randomUUID(),draft.versionId(),Timestamp.from(NOW)));
        assertThrows(org.springframework.dao.DataAccessException.class,() -> jdbc.update(
                "update knowledge_items set source_url='https://evil.example' where id=?",draft.itemId()));
        knowledge.archive(reviewer,draft.itemId(),1,"RETIRED");
        assertTrue(retriever.retrieve("MINDFULNESS","vi",3).isEmpty());
        assertEquals(1,jdbc.queryForObject("select count(*) from audit_logs where target_id=? and action='KNOWLEDGE_APPROVED'",
                Integer.class,draft.itemId()));
        assertEquals(1,jdbc.queryForObject("select count(*) from audit_logs where target_id=? and action='KNOWLEDGE_ARCHIVED'",
                Integer.class,draft.itemId()));

        mvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/users").with(jwt().jwt(j -> j.subject(ordinary.toString()))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/users").with(jwt().jwt(j -> j.subject(reviewer.toString()))
                .authorities(new SimpleGrantedAuthority("users:read-metadata"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").exists())
                .andExpect(jsonPath("$.items[0].email").doesNotExist());
        mvc.perform(get("/api/v1/admin/dashboard").with(jwt().jwt(j -> j.subject(reviewer.toString()))
                .authorities(new SimpleGrantedAuthority("admin:dashboard"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.suppressed").value(true))
                .andExpect(jsonPath("$.activeUsers").isEmpty());
        mvc.perform(post("/api/v1/admin/users/{id}:suspend",ordinary)
                .with(jwt().jwt(j -> j.subject(reviewer.toString()))
                        .authorities(new SimpleGrantedAuthority("users:suspend")))
                .contentType("application/json").content("{\"reasonCode\":\"ABUSE_REVIEW\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUSPENDED"));
        mvc.perform(post("/api/v1/admin/users/{id}:restore",ordinary)
                .with(jwt().jwt(j -> j.subject(reviewer.toString()))
                        .authorities(new SimpleGrantedAuthority("users:suspend")))
                .contentType("application/json").content("{\"reasonCode\":\"REVIEW_CLEARED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));

        UUID entryId=journal.create(ordinary,new CreateJournalEntryCommand("Synthetic entry",
                mapper.readTree("{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Synthetic\"}]}]}"),
                null,NOW,"Asia/Ho_Chi_Minh",null,null,null,null,null),
                "m6-entry-"+UUID.randomUUID()).id();
        jdbc.update("update journal_entries set analysis_status='ANALYSIS_FAILED' where id=?",entryId);
        UUID jobId=UUID.randomUUID();
        jdbc.update("""
                insert into ai_jobs(id,job_type,aggregate_type,aggregate_id,user_id,status,priority,attempt,
                    max_attempts,available_at,idempotency_key,payload,payload_version,last_error_code,
                    last_error_summary,created_at,finished_at)
                values (?,'JOURNAL_ANALYSIS','JOURNAL_ENTRY',?,?,'DEAD',100,3,3,?,
                    ?,cast(? as jsonb),1,'PROVIDER_UNAVAILABLE','raw sensitive failure',?,?)
                """,jobId,entryId,ordinary,Timestamp.from(NOW.minusSeconds(120)),
                "m6-job-"+jobId,"{\"contentVersion\":1,\"secret\":\"do-not-return\"}",Timestamp.from(NOW.minusSeconds(120)),
                Timestamp.from(NOW.minusSeconds(120)));
        mvc.perform(get("/api/v1/admin/ai-jobs")
                .with(jwt().jwt(j -> j.subject(reviewer.toString()))
                        .authorities(new SimpleGrantedAuthority("jobs:read"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].errorCode").value("PROVIDER_UNAVAILABLE"))
                .andExpect(result -> {
                    String body=result.getResponse().getContentAsString();
                    assertFalse(body.contains("do-not-return"));
                    assertFalse(body.contains("raw sensitive failure"));
                });
        mvc.perform(post("/api/v1/admin/ai-jobs/{id}:retry",jobId)
                .with(jwt().jwt(j -> j.subject(ordinary.toString()))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/ai-jobs/{id}:retry",jobId)
                .with(jwt().jwt(j -> j.subject(reviewer.toString()))
                        .authorities(new SimpleGrantedAuthority("jobs:retry"))))
                .andExpect(status().isAccepted());
        assertEquals("PENDING",jdbc.queryForObject("select status from ai_jobs where id=?",String.class,jobId));
        assertEquals("PENDING",jdbc.queryForObject("select analysis_status from journal_entries where id=?",String.class,entryId));
        assertEquals(1,jdbc.queryForObject("select count(*) from audit_logs where action='AI_JOB_RETRIED' and target_id=?",
                Integer.class,jobId));
        jdbc.update("delete from users where id=?",editor);
        jdbc.update("delete from users where id=?",reviewer);
        assertEquals("Hít thở chậm trong vài phút.\n\nTạm nghỉ khi cần.",knowledge.get(draft.itemId(),1).content());
        assertEquals(2,jdbc.queryForObject("select count(*) from knowledge_chunks where knowledge_version_id=?",
                Integer.class,draft.versionId()));
    }

    private UUID user() {
        UUID id=UUID.randomUUID(); String email="m6-"+id+"@example.test";
        var encrypted=cipher.encrypt("users.email",id,id,email);
        jdbc.update("""
                insert into users(id,email_lookup_hash,encrypted_email,email_iv,email_wrapped_key,email_key_version,
                    password_hash,auth_provider,status,created_at,updated_at)
                values (?,?,?,?,?,?,'test','LOCAL','ACTIVE',?,?)
                """,id,cipher.lookupHash(email),encrypted.ciphertext(),encrypted.iv(),encrypted.wrappedKey(),
                encrypted.keyVersion(),Timestamp.from(NOW),Timestamp.from(NOW));
        profiles.createDefault(id,"Asia/Ho_Chi_Minh","vi","v1","v1",NOW);
        return id;
    }
    private void role(UUID user,String code) {
        jdbc.update("insert into user_roles(user_id,role_id,assigned_at) select ?,id,? from roles where code=?",
                user,Timestamp.from(NOW),code);
    }
}
