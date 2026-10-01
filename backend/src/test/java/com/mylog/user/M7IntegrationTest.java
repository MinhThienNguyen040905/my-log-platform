package com.mylog.user;

import com.mylog.TestcontainersConfiguration;
import com.mylog.export.application.ExportService;
import com.mylog.feedback.application.FeedbackService;
import com.mylog.identity.application.IdentityStore;
import com.mylog.identity.application.IdentityService;
import com.mylog.identity.application.VerificationDelivery;
import com.mylog.journal.application.JournalAssetDeletion;
import com.mylog.journal.application.JournalService;
import com.mylog.journal.application.command.CreateJournalEntryCommand;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.user.application.AccountDeletionService;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

@Import({TestcontainersConfiguration.class,M7IntegrationTest.FixedClock.class})
@SpringBootTest(properties={"mylog.identity.enabled=true","mylog.exports.enabled=true","mylog.deletion.enabled=true",
        "spring.mail.host=localhost","MYLOG_MAIL_FROM=noreply@mylog.local","logging.level.root=WARN"})
@ActiveProfiles("test") @AutoConfigureMockMvc @Testcontainers(disabledWithoutDocker=true)
class M7IntegrationTest {
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods=false)
    static class FixedClock {
        @Bean @Primary Clock clockM7() {return Clock.fixed(Instant.parse("2026-10-10T00:00:00Z"),ZoneId.of("UTC"));}
    }
    private static final Instant NOW=Instant.parse("2026-10-10T00:00:00Z");
    private static final String PASSWORD="CorrectHorseBattery7!";
    @Autowired JdbcTemplate jdbc;
    @Autowired SensitiveDataCipher cipher;
    @Autowired UserProfileUseCase profiles;
    @Autowired ExportService exports;
    @Autowired AccountDeletionService deletion;
    @Autowired FeedbackService feedback;
    @Autowired IdentityStore identity;
    @Autowired IdentityService identityService;
    @Autowired MockMvc mvc;
    @Autowired JournalService journal;
    @Autowired ObjectMapper mapper;
    @MockitoBean VerificationDelivery delivery;
    @MockitoBean JournalAssetDeletion assets;

    @Test void writeQuotaPersistsAfterRejectionAndIsScopedByActionAndUser() {
        UUID owner=user(), other=user();
        for (int i=0;i<3;i++) identityService.checkWriteQuota(owner,"export",3,86400);
        assertThrows(com.mylog.platform.web.RateLimitExceededException.class,
                () -> identityService.checkWriteQuota(owner,"export",3,86400));
        assertEquals(4,jdbc.queryForObject("select attempts from auth_rate_limits where subject_hash=?",
                Integer.class,cipher.lookupHash("write:export:"+owner)));
        identityService.checkWriteQuota(other,"export",3,86400);
        identityService.checkWriteQuota(owner,"journal",60,900);
    }

    @Test void directPublicSchemaTablesRequireBackendAccess() {
        assertEquals(0,jdbc.queryForObject("""
                SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                WHERE n.nspname='public' AND c.relkind='r'
                  AND c.relname<>'flyway_schema_history' AND NOT c.relrowsecurity
                """,Integer.class));
    }

    @Test void exportIsEncryptedOwnerScopedAndGrantExpires() throws Exception {
        UUID owner=user(), stranger=user();
        feedback.submit(owner,"OTHER","Nhật ký thử nghiệm 😀");
        var deletedEntry=journal.create(owner,new CreateJournalEntryCommand("Retained soft deleted journal",
                mapper.readTree("{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"A past day\"}]}]}"),
                null,NOW,"Asia/Ho_Chi_Minh",null,null,null,null,null),
                "m7-soft-"+UUID.randomUUID());
        journal.delete(owner,deletedEntry.id(),deletedEntry.version());
        var request=exports.request(owner,"CSV");
        assertThrows(Exception.class,()->exports.get(stranger,request.id()));
        exports.poll();
        assertEquals("READY",exports.get(owner,request.id()).status());
        assertFalse(new String(jdbc.queryForObject("select encrypted_file from export_requests where id=?",byte[].class,request.id()))
                .contains("@example.test"));
        var grant=exports.authorizeDownload(owner,request.id(),PASSWORD);
        String query=grant.url().substring(grant.url().indexOf('?')+1);
        String[] parts=query.split("&");
        long expires=Long.parseLong(parts[0].substring("expires=".length()));
        String signature=parts[1].substring("signature=".length());
        String csv=new String(exports.download(owner,request.id(),expires,signature).bytes(),java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(csv.contains("@example.test"));
        assertTrue(csv.contains("Retained soft deleted journal"));
        mvc.perform(get("/api/v1/exports/{id}/file",request.id())
                .param("expires",Long.toString(expires)).param("signature",signature))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/exports/{id}/file",request.id())
                .param("expires",Long.toString(expires)).param("signature",signature)
                .with(jwt().jwt(j->j.subject(stranger.toString()))))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/exports/{id}/file",request.id())
                .param("expires",Long.toString(expires)).param("signature",signature)
                .with(jwt().jwt(j->j.subject(owner.toString()))))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
        assertThrows(Exception.class,()->exports.download(stranger,request.id(),expires,signature));
        assertThrows(Exception.class,()->exports.download(owner,request.id(),NOW.getEpochSecond()-1,signature));
        assertThrows(Exception.class,()->exports.authorizeDownload(owner,request.id(),"wrong"));
        jdbc.update("update export_requests set expires_at=? where id=?",Timestamp.from(NOW.minusSeconds(1)),request.id());
        assertThrows(Exception.class,()->exports.download(owner,request.id(),expires,signature));
        var pdf=exports.request(owner,"PDF"); // expired artifact no longer blocks another request
        exports.poll();
        assertEquals("READY",exports.get(owner,pdf.id()).status());
        var pdfGrant=exports.authorizeDownload(owner,pdf.id(),PASSWORD);
        String pdfQuery=pdfGrant.url().substring(pdfGrant.url().indexOf('?')+1);
        String[] pdfParts=pdfQuery.split("&");
        byte[] pdfBytes=exports.download(owner,pdf.id(),Long.parseLong(pdfParts[0].substring(8)),
                pdfParts[1].substring(10)).bytes();
        assertTrue(new String(pdfBytes,0,4,java.nio.charset.StandardCharsets.US_ASCII).startsWith("%PDF"));
    }

    @Test void deletionCanBeCancelledAndWorkerPurgesOwnedRows() throws Exception {
        UUID owner=user(); String email="m7-"+owner+"@example.test";
        UUID entry=journal.create(owner,new CreateJournalEntryCommand("Synthetic diary",
                mapper.readTree("{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"A calm day\"}]}]}"),
                null,NOW,"Asia/Ho_Chi_Minh",null,null,null,null,null),
                "m7-entry-"+UUID.randomUUID()).id();
        UUID session=UUID.randomUUID();
        jdbc.update("""
                insert into auth_sessions(id,user_id,token_family_id,current_token_hash,last_used_at,expires_at,created_at)
                values (?,?,?,?,?,?,?)
                """,session,owner,UUID.randomUUID(),cipher.tokenHash("m7-session-"+session),
                Timestamp.from(NOW),Timestamp.from(NOW.plusSeconds(3600)),Timestamp.from(NOW));
        var first=deletion.request(owner,PASSWORD);
        assertEquals("DELETION_PENDING",jdbc.queryForObject("select status from users where id=?",String.class,owner));
        assertNotNull(jdbc.queryForObject("select revoked_at from auth_sessions where id=?",Timestamp.class,session));
        mvc.perform(post("/api/v1/account-deletion-requests/{id}:status",first.id())
                .contentType("application/json").content("{\"email\":\""+email+"\",\"password\":\""+PASSWORD+"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("GRACE_PERIOD"));
        mvc.perform(post("/api/v1/account-deletion-requests/{id}:cancel",first.id())
                .contentType("application/json").content("{\"email\":\""+email+"\",\"password\":\""+PASSWORD+"\"}"))
                .andExpect(status().isNoContent());
        assertEquals("ACTIVE",jdbc.queryForObject("select status from users where id=?",String.class,owner));
        var second=deletion.request(owner,PASSWORD);
        jdbc.update("update deletion_requests set scheduled_for=? where id=?",Timestamp.from(NOW.minusSeconds(1)),second.id());
        doThrow(new IllegalStateException("synthetic provider outage")).doNothing().when(assets).deleteAll(owner);
        deletion.poll();
        assertEquals("FAILED",jdbc.queryForObject("select status from deletion_requests where id=?",String.class,second.id()));
        assertEquals(1,jdbc.queryForObject("select count(*) from users where id=?",Integer.class,owner));
        jdbc.update("update deletion_requests set scheduled_for=? where id=?",Timestamp.from(NOW.minusSeconds(1)),second.id());
        deletion.poll();
        assertEquals(0,jdbc.queryForObject("select count(*) from users where id=?",Integer.class,owner));
        assertEquals(0,jdbc.queryForObject("select count(*) from auth_sessions where user_id=?",Integer.class,owner));
        assertEquals(0,jdbc.queryForObject("select count(*) from user_profiles where user_id=?",Integer.class,owner));
        assertEquals(0,jdbc.queryForObject("select count(*) from journal_entries where id=?",Integer.class,entry));
        assertEquals(0,jdbc.queryForObject("select count(*) from safety_events where user_id=?",Integer.class,owner));
        assertEquals(0,jdbc.queryForObject("select count(*) from outbox_events where aggregate_id=?",Integer.class,entry));
        assertEquals("COMPLETED",jdbc.queryForObject("select status from deletion_requests where id=?",String.class,second.id()));
    }

    @Test void feedbackEncryptedAndAdminReadRequiresPermission() throws Exception {
        UUID owner=user(), other=user();
        UUID id=feedback.submit(owner,"BUG","Synthetic private feedback");
        assertFalse(new String(jdbc.queryForObject("select encrypted_message from feedback where id=?",byte[].class,id))
                .contains("Synthetic private feedback"));
        mvc.perform(get("/api/v1/feedback/{id}",id).with(jwt().jwt(j->j.subject(other.toString()))))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/admin/feedback/{id}",id).with(jwt().jwt(j->j.subject(other.toString()))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/feedback/{id}",id).with(jwt().jwt(j->j.subject(other.toString()))
                .authorities(new SimpleGrantedAuthority("feedback:read"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Synthetic private feedback"));
        mvc.perform(get("/api/v1/admin/feedback").with(jwt().jwt(j->j.subject(other.toString()))
                .authorities(new SimpleGrantedAuthority("feedback:read"))))
                .andExpect(status().isOk()).andExpect(result ->
                        assertTrue(result.getResponse().getContentAsString().contains(id.toString())));
        assertEquals(2,jdbc.queryForObject("select count(*) from audit_logs where action='FEEDBACK_READ' and target_id=?",
                Integer.class,id));
        mvc.perform(patch("/api/v1/admin/feedback/{id}",id).with(jwt().jwt(j->j.subject(other.toString()))
                .authorities(new SimpleGrantedAuthority("feedback:read")))
                .header("If-Match","0")
                .contentType("application/json").content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isForbidden());
        jdbc.update("insert into user_roles(user_id,role_id,assigned_at) select ?,id,? from roles where code='SUPPORT_AGENT'",
                other,Timestamp.from(NOW));
        mvc.perform(patch("/api/v1/admin/feedback/{id}",id).with(jwt().jwt(j->j.subject(other.toString()))
                .authorities(new SimpleGrantedAuthority("feedback:manage")))
                .header("If-Match","0")
                .contentType("application/json").content("{\"status\":\"IN_PROGRESS\",\"assignedTo\":\""+other+"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.assignedTo").value(other.toString()))
                .andExpect(jsonPath("$.version").value(1));
        mvc.perform(patch("/api/v1/admin/feedback/{id}",id).with(jwt().jwt(j->j.subject(other.toString()))
                .authorities(new SimpleGrantedAuthority("feedback:manage")))
                .header("If-Match","0")
                .contentType("application/json").content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isConflict());
        assertEquals(1,jdbc.queryForObject("select count(*) from audit_logs where action='FEEDBACK_UPDATED' and target_id=?",
                Integer.class,id));
        jdbc.update("update feedback set expires_at=? where id=?",Timestamp.from(NOW.minusSeconds(1)),id);
        feedback.cleanup();
        assertEquals(0,jdbc.queryForObject("select count(*) from feedback where id=?",Integer.class,id));
    }

    private UUID user() {
        UUID id=UUID.randomUUID();String email="m7-"+id+"@example.test";
        var encrypted=cipher.encrypt("users.email",id,id,email);
        jdbc.update("""
                insert into users(id,email_lookup_hash,encrypted_email,email_iv,email_wrapped_key,email_key_version,
                    password_hash,auth_provider,status,created_at,updated_at)
                values (?,?,?,?,?,? ,?,'LOCAL','ACTIVE',?,?)
                """,id,cipher.lookupHash(email),encrypted.ciphertext(),encrypted.iv(),encrypted.wrappedKey(),
                encrypted.keyVersion(),new BCryptPasswordEncoder(12).encode(PASSWORD),Timestamp.from(NOW),Timestamp.from(NOW));
        profiles.createDefault(id,"Asia/Ho_Chi_Minh","vi","v1","v1",NOW);
        return id;
    }
}
