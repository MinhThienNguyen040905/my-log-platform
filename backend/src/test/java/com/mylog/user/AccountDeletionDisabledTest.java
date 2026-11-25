package com.mylog.user;

import com.mylog.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC-23 is off by default: no endpoint (404) and no worker bean. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {"mylog.identity.enabled=true", "spring.mail.host=localhost",
        "MYLOG_MAIL_FROM=noreply@mylog.local", "logging.level.root=WARN"})
@ActiveProfiles("test") @AutoConfigureMockMvc @Testcontainers(disabledWithoutDocker = true)
class AccountDeletionDisabledTest {
    @Autowired MockMvc mvc;
    @Autowired ApplicationContext context;

    @Test void endpointAndWorkerAreAbsentByDefault() throws Exception {
        mvc.perform(post("/api/v1/account-deletion-requests")
                        .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString())))
                        .contentType("application/json").content("{\"password\":\"Synthetic-Password-1\"}"))
                .andExpect(status().isNotFound());
        assertEquals(0, context.getBeanNamesForType(com.mylog.user.application.AccountDeletionService.class).length);
        assertEquals(1, context.getBeanNamesForType(com.mylog.identity.application.PendingDeletionCanceller.class).length,
                "signing in still cancels requests made while the feature was on");
    }
}
