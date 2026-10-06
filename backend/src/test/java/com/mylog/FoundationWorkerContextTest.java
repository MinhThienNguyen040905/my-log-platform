package com.mylog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,"
                + "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration,"
                + "org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration",
        "mylog.app-profile=worker",
        "mylog.open-api.enabled=true",
        "springdoc.api-docs.enabled=true"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class FoundationWorkerContextTest {
    @Autowired MockMvc mockMvc;

    @Test void workerExposesHealthButDeniesApiAndDocumentation() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mockMvc.perform(get("/internal/openapi")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/safety/resources")).andExpect(status().isUnauthorized());
    }
}
