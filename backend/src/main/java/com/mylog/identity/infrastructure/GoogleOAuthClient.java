package com.mylog.identity.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mylog.identity.application.IdentityService;
import com.mylog.platform.config.MylogProperties;
import com.mylog.platform.web.DependencyUnavailableException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class GoogleOAuthClient {
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final List<String> VALID_ISSUERS = List.of("https://accounts.google.com", "accounts.google.com");

    private final MylogProperties properties;
    private final RestClient restClient;
    private final JwtDecoder jwtDecoder;

    public record GoogleTokenPayload(String sub, String email, boolean emailVerified, String name) {
    }

    record GoogleTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("id_token") String idToken,
            @JsonProperty("expires_in") Integer expiresIn,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("scope") String scope
    ) {
    }

    public GoogleOAuthClient(MylogProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
        this.jwtDecoder = NimbusJwtDecoder.withJwkSetUri(JWKS_URI).build();
    }

    public GoogleTokenPayload exchangeCode(String code) {
        var googleConfig = properties.identity().oauth().google();
        if (!googleConfig.enabled() || googleConfig.clientId() == null || googleConfig.clientId().isBlank()
                || googleConfig.clientSecret() == null || googleConfig.clientSecret().isBlank()) {
            throw new IdentityService.InvalidCredentialsException();
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", googleConfig.clientId());
        body.add("client_secret", googleConfig.clientSecret());
        body.add("redirect_uri", "postmessage");
        body.add("grant_type", "authorization_code");

        GoogleTokenResponse tokenResponse;
        try {
            tokenResponse = restClient.post()
                    .uri(URI.create(TOKEN_URL))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(GoogleTokenResponse.class);
        } catch (HttpClientErrorException.BadRequest e) {
            // Google invalid_grant (code invalid or expired)
            throw new IdentityService.InvalidCredentialsException();
        } catch (Exception e) {
            // Network failure or Google 5xx
            throw new DependencyUnavailableException();
        }

        if (tokenResponse == null || tokenResponse.idToken() == null) {
            throw new IdentityService.InvalidCredentialsException();
        }

        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(tokenResponse.idToken());
        } catch (JwtException e) {
            throw new IdentityService.InvalidCredentialsException();
        }

        String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : "";
        if (!VALID_ISSUERS.contains(issuer)) {
            throw new IdentityService.InvalidCredentialsException();
        }

        List<String> audience = jwt.getAudience();
        if (audience == null || !audience.contains(googleConfig.clientId())) {
            throw new IdentityService.InvalidCredentialsException();
        }

        String sub = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");
        String name = jwt.getClaimAsString("name");

        if (sub == null || sub.isBlank() || email == null || email.isBlank() || !Boolean.TRUE.equals(emailVerified)) {
            throw new IdentityService.InvalidCredentialsException();
        }

        return new GoogleTokenPayload(sub, email, true, name != null ? name : "");
    }
}
