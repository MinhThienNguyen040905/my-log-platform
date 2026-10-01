package com.mylog.platform.security;

import com.mylog.platform.config.MylogProperties;
import com.mylog.identity.infrastructure.IdentityJwt;
import com.mylog.identity.infrastructure.IdentityAuthenticationConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            MylogProperties properties,
            ProblemAuthenticationEntryPoint authenticationEntryPoint,
            ProblemAccessDeniedHandler accessDeniedHandler,
            ObjectProvider<IdentityJwt> identityJwt,
            ObjectProvider<IdentityAuthenticationConverter> identityConverter
    ) throws Exception {
        boolean identityEnabled = properties.identity().enabled();
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
                    authorize.requestMatchers(HttpMethod.GET, "/api/v1/safety/resources").permitAll();
                    if (properties.openApi().enabled()) {
                        authorize.requestMatchers(
                                "/internal/openapi", "/internal/openapi/**",
                                "/internal/swagger-ui", "/internal/swagger-ui/**",
                                "/swagger-ui/**"
                        ).permitAll();
                    }
                    if (identityEnabled) {
                        authorize.requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh",
                                "/api/v1/auth/email-verifications", "/api/v1/auth/email-verifications:confirm")
                                .permitAll();
                        authorize.requestMatchers("/api/v1/auth/logout", "/api/v1/me", "/api/v1/me/**",
                                        "/api/v1/journal-entries", "/api/v1/journal-entries/**",
                                        "/api/v1/journal-tags", "/api/v1/journal-tags/**",
                                        "/api/v1/check-ins", "/api/v1/check-ins/**")
                                .authenticated();
                        authorize.requestMatchers("/api/v1/dashboard", "/api/v1/insights",
                                        "/api/v1/reports", "/api/v1/reports/**")
                                .authenticated();
                    }
                    authorize.anyRequest().denyAll();
                })
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .requestCache(cache -> cache.disable());
        if (identityEnabled) {
            http.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                    .decoder(identityJwt.getObject())
                    .jwtAuthenticationConverter(identityConverter.getObject())));
        }
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(MylogProperties properties) {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.web().allowedOrigins());
        configuration.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        ));
        configuration.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT,
                "Idempotency-Key",
                HttpHeaders.IF_MATCH
        ));
        configuration.setExposedHeaders(List.of(HttpHeaders.ETAG, "X-Request-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
