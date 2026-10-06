package com.mylog.platform.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ErrorTestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, ApiProblemFactory.class, GlobalExceptionHandlerTest.FixedClockConfiguration.class})
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {
    private final MockMvc mockMvc;

    @Autowired
    GlobalExceptionHandlerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void returnsValidationProblemWithoutRejectedValue() throws Exception {
        mockMvc.perform(post("/test/errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be blank"));
    }

    @Test
    void returnsMalformedRequestWithoutParserDetails() throws Exception {
        mockMvc.perform(post("/test/errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.detail").value("Cấu trúc hoặc kiểu dữ liệu của yêu cầu không hợp lệ."));
    }

    @Test
    void returnsStableApplicationErrorCodes() throws Exception {
        assertProblem("not-found", 404, "RESOURCE_NOT_FOUND");
        assertProblem("conflict", 409, "CONFLICT");
        assertProblem("rate-limit", 429, "RATE_LIMITED");
        assertProblem("dependency", 503, "DEPENDENCY_UNAVAILABLE");
        assertProblem("unauthorized", 401, "UNAUTHORIZED");
        assertProblem("forbidden", 403, "FORBIDDEN");
    }

    @Test
    void unexpectedErrorNeverReturnsExceptionMessage() throws Exception {
        mockMvc.perform(get("/test/errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("Hệ thống gặp lỗi không mong đợi."))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("database-password-and-sql"))));
    }

    private void assertProblem(String path, int expectedStatus, String expectedCode) throws Exception {
        mockMvc.perform(get("/test/errors/" + path))
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(expectedCode))
                .andExpect(jsonPath("$.timestamp").value("2026-09-30T03:00:00Z"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {
        @Bean
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-09-30T03:00:00Z"), ZoneOffset.UTC);
        }

        @Bean
        com.mylog.platform.id.IdGenerator fixedIdGenerator() {
            return () -> java.util.UUID.fromString("019d0528-cc00-7000-8000-000000000001");
        }
    }
}
