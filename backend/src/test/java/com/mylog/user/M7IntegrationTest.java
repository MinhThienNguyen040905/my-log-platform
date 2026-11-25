package com.mylog.user;

import com.mylog.TestcontainersConfiguration;
import com.mylog.export.application.ExportService;
import com.mylog.feedback.application.FeedbackService;
import com.mylog.identity.application.IdentityStore;
import com.mylog.identity.application.IdentityService;
import com.mylog.identity.application.VerificationDelivery;
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
@SpringBootTest(properties={"mylog.identity.enabled=true","mylog.exports.enabled=true","mylog.account-deletion.enabled=true",
        // Tests drive the workers by hand; keep the schedulers from racing them.
        "mylog.account-deletion.poll-delay-ms=3600000","mylog.assets.deletion-queue-delay-ms=3600000",
        "mylog.exports.poll-delay-ms=3600000",
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
        var grant=exports.authorizeDownload(owner,null,request.id(),PASSWORD);
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
        assertThrows(Exception.class,()->exports.authorizeDownload(owner,null,request.id(),"wrong"));
        jdbc.update("update export_requests set expires_at=? where id=?",Timestamp.from(NOW.minusSeconds(1)),request.id());
        assertThrows(Exception.class,()->exports.download(owner,request.id(),expires,signature));
        var pdf=exports.request(owner,"PDF"); // expired artifact no longer blocks another request
        exports.poll();
        assertEquals("READY",exports.get(owner,pdf.id()).status());
        var pdfGrant=exports.authorizeDownload(owner,null,pdf.id(),PASSWORD);
        String pdfQuery=pdfGrant.url().substring(pdfGrant.url().indexOf('?')+1);
        String[] pdfParts=pdfQuery.split("&");
        byte[] pdfBytes=exports.download(owner,pdf.id(),Long.parseLong(pdfParts[0].substring(8)),
                pdfParts[1].substring(10)).bytes();
        assertTrue(new String(pdfBytes,0,4,java.nio.charset.StandardCharsets.US_ASCII).startsWith("%PDF"));
    }

    @Test void exportDownloadConfirmsIdentityForPasswordAndGoogleAccounts() throws Exception {
        UUID local=user(), google=googleUser();
        var localExport=exports.request(local,"CSV");
        var googleExport=exports.request(google,"CSV");
        exports.poll(); exports.poll();
        UUID localSession=session(local,NOW);
        for (int i=0;i<5;i++)
            mvc.perform(authorize(local,localSession,localExport.id(),"WrongPassword123!"))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail").value("PASSWORD_INCORRECT"));
        mvc.perform(authorize(local,localSession,localExport.id(),PASSWORD)).andExpect(status().isTooManyRequests());

        UUID stale=session(google,NOW.minusSeconds(3600));
        mvc.perform(authorize(google,stale,googleExport.id(),null))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail").value("RECENT_LOGIN_REQUIRED"));
        UUID fresh=session(google,NOW.minusSeconds(120));
        mvc.perform(authorize(google,fresh,googleExport.id(),null))
                .andExpect(status().isOk()).andExpect(jsonPath("$.url").exists());
    }

    @Test void deletionRequestNeedsPasswordSignsOutAndSigningInAgainCancels() throws Exception {
        UUID owner=user(); String email="m7-"+owner+"@example.test";
        UUID session=session(owner,NOW);
        mvc.perform(requestDeletion(owner,session,"WrongPassword123!"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail").value("PASSWORD_INCORRECT"));
        assertEquals("ACTIVE",jdbc.queryForObject("select status from users where id=?",String.class,owner));

        mvc.perform(requestDeletion(owner,session,PASSWORD)).andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("GRACE_PERIOD"))
                .andExpect(jsonPath("$.scheduledFor").value(NOW.plusSeconds(7*86400).toString()));
        assertEquals("DELETION_PENDING",jdbc.queryForObject("select status from users where id=?",String.class,owner));
        assertNotNull(jdbc.queryForObject("select revoked_at from auth_sessions where id=?",Timestamp.class,session));
        verify(delivery).sendAccountDeletionScheduled(email,NOW.plusSeconds(7*86400));

        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                        .content("{\"email\":\""+email+"\",\"password\":\""+PASSWORD+"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.deletionCancelled").value(true));
        assertEquals("ACTIVE",jdbc.queryForObject("select status from users where id=?",String.class,owner));
        assertEquals("CANCELLED",jdbc.queryForObject("select status from deletion_requests where user_id=?",String.class,owner));
        assertEquals(1,jdbc.queryForObject("select count(*) from audit_logs where action='ACCOUNT_DELETION_CANCELLED' and target_id=?",
                Integer.class,owner));
        // A later sign-in is an ordinary one.
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                        .content("{\"email\":\""+email+"\",\"password\":\""+PASSWORD+"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.deletionCancelled").value(false));
    }

    @Test void signingInAfterTheGracePeriodDoesNotCancel() throws Exception {
        UUID owner=user(); String email="m7-"+owner+"@example.test";
        var request=deletion.request(owner,null,PASSWORD);
        jdbc.update("update deletion_requests set scheduled_for=? where id=?",Timestamp.from(NOW.minusSeconds(1)),request.id());
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                        .content("{\"email\":\""+email+"\",\"password\":\""+PASSWORD+"\"}"))
                .andExpect(status().isUnauthorized());
        assertEquals("DELETION_PENDING",jdbc.queryForObject("select status from users where id=?",String.class,owner));
        assertEquals("GRACE_PERIOD",jdbc.queryForObject("select status from deletion_requests where id=?",String.class,request.id()));
    }

    @Test void workerPurgesUserQueuesFilesKeepsAdminAuditAndEmails() throws Exception {
        UUID owner=user(), admin=user(); String email="m7-"+owner+"@example.test";
        UUID entry=journal.create(owner,new CreateJournalEntryCommand("Synthetic diary",
                mapper.readTree("{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"A calm day\"}]}]}"),
                null,NOW,"Asia/Ho_Chi_Minh",null,null,null,null,null),
                "m7-entry-"+UUID.randomUUID()).id();
        String publicId="mylog/"+owner+"/"+UUID.randomUUID();
        asset(owner,entry,publicId);
        jdbc.update("insert into audit_logs(id,actor_user_id,actor_type,action,target_type,target_id,reason_code,occurred_at)"
                + " values (?,?,'ADMIN','USER_SUSPENDED','USER',?,'SECURITY_VIOLATION',?)",UUID.randomUUID(),admin,owner,Timestamp.from(NOW));
        var request=deletion.request(owner,null,PASSWORD);
        jdbc.update("update deletion_requests set scheduled_for=? where id=?",Timestamp.from(NOW.minusSeconds(1)),request.id());

        deletion.poll();

        assertEquals("COMPLETED",jdbc.queryForObject("select status from deletion_requests where id=?",String.class,request.id()));
        assertEquals(0,jdbc.queryForObject("select count(*) from users where id=?",Integer.class,owner));
        assertEquals(0,jdbc.queryForObject("select count(*) from auth_sessions where user_id=?",Integer.class,owner));
        assertEquals(0,jdbc.queryForObject("select count(*) from user_profiles where user_id=?",Integer.class,owner));
        assertEquals(0,jdbc.queryForObject("select count(*) from journal_entries where id=?",Integer.class,entry));
        assertEquals(0,jdbc.queryForObject("select count(*) from journal_assets where user_id=?",Integer.class,owner));
        assertEquals(0,jdbc.queryForObject("select count(*) from outbox_events where aggregate_id=?",Integer.class,entry));
        assertEquals(1,jdbc.queryForObject("select count(*) from asset_deletion_queue where public_id=?",Integer.class,publicId),
                "the stored file is queued for deletion");
        assertEquals(1,jdbc.queryForObject("select count(*) from audit_logs where action='USER_SUSPENDED' and target_id=?",
                Integer.class,owner),"admin audit is kept");
        assertEquals(0,jdbc.queryForObject("select count(*) from audit_logs where actor_user_id=? and actor_type='USER'",
                Integer.class,owner),"the user's own audit is deleted");
        verify(delivery).sendAccountDeleted(email);
    }

    @Test void failedPurgeRollsBackTheFileQueueAndRetries() throws Exception {
        UUID owner=user();
        UUID entry=journal.create(owner,new CreateJournalEntryCommand("Synthetic diary",
                mapper.readTree("{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"A calm day\"}]}]}"),
                null,NOW,"Asia/Ho_Chi_Minh",null,null,null,null,null),
                "m7-entry-"+UUID.randomUUID()).id();
        String publicId="mylog/"+owner+"/"+UUID.randomUUID();
        asset(owner,entry,publicId);
        var request=deletion.request(owner,null,PASSWORD);
        jdbc.update("update deletion_requests set scheduled_for=? where id=?",Timestamp.from(NOW.minusSeconds(1)),request.id());
        // Breaks the purge precondition so the transaction fails after the files were queued.
        jdbc.update("update users set status='ACTIVE' where id=?",owner);

        deletion.poll();

        assertEquals("FAILED",jdbc.queryForObject("select status from deletion_requests where id=?",String.class,request.id()));
        assertEquals("CLEANUP_RETRY",jdbc.queryForObject("select last_error_code from deletion_requests where id=?",String.class,request.id()));
        assertEquals(1,jdbc.queryForObject("select count(*) from users where id=?",Integer.class,owner));
        assertEquals(0,jdbc.queryForObject("select count(*) from asset_deletion_queue where public_id=?",Integer.class,publicId),
                "queueing rolls back with the failed purge");
        verify(delivery,never()).sendAccountDeleted(any());
    }

    @Test void feedbackEncryptedAndAdminReadRequiresPermission() throws Exception {
        UUID owner=user(), other=user();
        UUID id=feedback.submit(owner,"BUG","Synthetic private feedback");
        assertFalse(new String(jdbc.queryForObject("select encrypted_message from feedback where id=?",byte[].class,id))
                .contains("Synthetic private feedback"));
        mvc.perform(get("/api/v1/feedback/{id}",id).with(jwt().jwt(j->j.subject(other.toString()))))
                .andExpect(status().isNotFound());
        UUID newer=feedback.submit(owner,"IDEA","Synthetic newer idea");
        mvc.perform(get("/api/v1/feedback").with(jwt().jwt(j->j.subject(owner.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(newer.toString()))
                .andExpect(jsonPath("$[1].message").value("Synthetic private feedback"));
        mvc.perform(get("/api/v1/feedback").with(jwt().jwt(j->j.subject(other.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
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

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder requestDeletion(
            UUID user,UUID session,String password) {
        return post("/api/v1/account-deletion-requests")
                .with(jwt().jwt(j->j.subject(user.toString()).claim("sid",session.toString())))
                .contentType("application/json").content("{\"password\":\""+password+"\"}");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authorize(
            UUID user,UUID session,UUID exportId,String password) {
        return post("/api/v1/exports/{id}:authorize-download",exportId)
                .with(jwt().jwt(j->j.subject(user.toString()).claim("sid",session.toString())))
                .contentType("application/json").content(password==null ? "{}" : "{\"password\":\""+password+"\"}");
    }

    private UUID googleUser() {
        UUID id=UUID.randomUUID();String email="m7-google-"+id+"@example.test";
        var encrypted=cipher.encrypt("users.email",id,id,email);
        jdbc.update("""
                insert into users(id,email_lookup_hash,encrypted_email,email_iv,email_wrapped_key,email_key_version,
                    password_hash,auth_provider,status,created_at,updated_at)
                values (?,?,?,?,?,?,NULL,'GOOGLE','ACTIVE',?,?)
                """,id,cipher.lookupHash(email),encrypted.ciphertext(),encrypted.iv(),encrypted.wrappedKey(),
                encrypted.keyVersion(),Timestamp.from(NOW),Timestamp.from(NOW));
        profiles.createDefault(id,"Asia/Ho_Chi_Minh","vi","v1","v1",NOW);
        return id;
    }

    private UUID session(UUID user,Instant createdAt) {
        UUID id=UUID.randomUUID();
        jdbc.update("""
                insert into auth_sessions(id,user_id,token_family_id,current_token_hash,last_used_at,expires_at,created_at)
                values (?,?,?,?,?,?,?)
                """,id,user,UUID.randomUUID(),cipher.tokenHash("m7-session-"+id),
                Timestamp.from(createdAt),Timestamp.from(NOW.plusSeconds(3600)),Timestamp.from(createdAt));
        return id;
    }

    private void asset(UUID user,UUID entry,String publicId) {
        jdbc.update("""
                insert into journal_assets(id,journal_entry_id,user_id,provider_asset_id,public_id,provider_version,format,
                    delivery_type,asset_type,mime_type,size_bytes,sha256,status,created_at)
                values (?,?,?,?,?,1,'webp','AUTHENTICATED','IMAGE','image/webp',10,?,'READY',?)
                """,UUID.randomUUID(),entry,user,"provider-"+UUID.randomUUID(),publicId,new byte[32],Timestamp.from(NOW));
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
