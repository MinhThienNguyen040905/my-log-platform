package com.mylog.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mylog.common.security.JwtProperties;
import com.mylog.support.AbstractIntegrationTest;
import com.mylog.identity.service.AuthRateLimiter;
import com.mylog.common.exception.RateLimitExceededException;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "spring.main.web-application-type=servlet")
@AutoConfigureMockMvc
@ActiveProfiles({"api", "integration-test"})
class IdentityApiIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "StrongPass123";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private JwtEncoder jwtEncoder;
    @Autowired private JwtProperties jwtProperties;
    @Autowired private AuthRateLimiter authRateLimiter;

    @BeforeEach
    void cleanState() {
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }

    @Test
    void registerNormalizesEmailHashesSecretsAndRejectsDuplicate() throws Exception {
        Registration registration = register("Test.User@Example.com", "Test User");

        assertThat(registration.accessToken()).isNotBlank();
        assertThat(registration.refreshCookie().isHttpOnly()).isTrue();
        assertThat(registration.refreshCookie().getValue()).isNotBlank();

        Map<String, Object> userRow = jdbcTemplate.queryForMap(
                "SELECT email_normalized, password_hash FROM users");
        assertThat(userRow.get("email_normalized")).isEqualTo("test.user@example.com");
        assertThat(userRow.get("password_hash").toString())
                .startsWith("$2")
                .isNotEqualTo(PASSWORD);

        String tokenHash = jdbcTemplate.queryForObject(
                "SELECT token_hash FROM refresh_tokens", String.class);
        assertThat(tokenHash)
                .hasSize(64)
                .isNotEqualTo(registration.refreshCookie().getValue());
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM user_consents", Integer.class))
                .isEqualTo(2);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "  TEST.USER@example.com ",
                                "password", PASSWORD,
                                "displayName", "Other",
                                "acceptTerms", true,
                                "acceptPrivacy", true))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void validatesPasswordPolicyAndConsent() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "weak@example.com",
                                "password", "weakpassword",
                                "displayName", "Weak",
                                "acceptTerms", false,
                                "acceptPrivacy", true))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors").isArray());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "unicode@example.com",
                                "password", "あ".repeat(25)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void loginSucceedsWithNormalizedEmailAndUsesGenericFailure() throws Exception {
        register("login@example.com", "Login User");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", " LOGIN@example.com ",
                                "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(cookie().httpOnly("mylog_refresh", true));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "unknown@example.com",
                                "password", "WrongPassword123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Email or password is invalid"));
    }

    @Test
    void protectedEndpointRejectsMissingInvalidAndExpiredAccessTokens() throws Exception {
        Registration registration = register("expired@example.com", "Expired User");

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject(registration.userId().toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(now.minusSeconds(300))
                .expiresAt(now.minusSeconds(120))
                .claim("roles", List.of("ROLE_USER"))
                .build();
        String expiredToken = jwtEncoder.encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotatesTokenAndReuseRevokesTheWholeFamily() throws Exception {
        Registration registration = register("rotate@example.com", "Rotate User");
        Cookie original = registration.refreshCookie();

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh").cookie(original))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("mylog_refresh"))
                .andReturn();
        Cookie rotated = refreshResult.getResponse().getCookie("mylog_refresh");
        assertThat(rotated).isNotNull();
        assertThat(rotated.getValue()).isNotEqualTo(original.getValue());

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(original))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(rotated))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void logoutRevokesSessionAndClearsCookie() throws Exception {
        Registration registration = register("logout@example.com", "Logout User");

        mockMvc.perform(post("/api/v1/auth/logout").cookie(registration.refreshCookie()))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(registration.refreshCookie()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meEndpointsAreAlwaysScopedToAuthenticatedUser() throws Exception {
        Registration first = register("first@example.com", "First User");
        Registration second = register("second@example.com", "Second User");

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + first.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "displayName", "First Updated",
                                "timezone", "Asia/Ho_Chi_Minh",
                                "language", "en"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(first.userId().toString()))
                .andExpect(jsonPath("$.displayName").value("First Updated"));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + second.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(second.userId().toString()))
                .andExpect(jsonPath("$.displayName").value("Second User"));
    }

    @Test
    void lockedAccountCannotContinueUsingAnExistingAccessToken() throws Exception {
        Registration registration = register("locked@example.com", "Locked User");
        jdbcTemplate.update("UPDATE users SET status = 'LOCKED' WHERE id = ?", registration.userId());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + registration.accessToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void registerRateLimitIsEnforcedAtomicallyInRedis() {
        for (int attempt = 0; attempt < 100; attempt++) {
            authRateLimiter.checkRegister("203.0.113.10");
        }

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> authRateLimiter.checkRegister("203.0.113.10"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    private Registration register(String email, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "password", PASSWORD,
                                "displayName", displayName,
                                "acceptTerms", true,
                                "acceptPrivacy", true))))
                .andExpect(status().isCreated())
                .andExpect(cookie().exists("mylog_refresh"))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new Registration(
                UUID.fromString(body.get("user").get("id").asText()),
                body.get("accessToken").asText(),
                result.getResponse().getCookie("mylog_refresh"));
    }

    private String json(Object value) {
        return objectMapper.writeValueAsString(value);
    }

    private record Registration(UUID userId, String accessToken, Cookie refreshCookie) {}
}
