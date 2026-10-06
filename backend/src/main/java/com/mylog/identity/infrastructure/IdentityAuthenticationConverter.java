package com.mylog.identity.infrastructure;

import com.mylog.identity.application.IdentityStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayList;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public final class IdentityAuthenticationConverter implements Converter<Jwt, JwtAuthenticationToken> {
    private final IdentityStore store;
    private final Clock clock;
    IdentityAuthenticationConverter(IdentityStore store, Clock clock) { this.store = store; this.clock = clock; }

    @Override public JwtAuthenticationToken convert(Jwt jwt) {
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            UUID sessionId = UUID.fromString(jwt.getClaimAsString("sid"));
            var account = store.accountById(userId).orElseThrow();
            var session = store.sessionById(sessionId).orElseThrow();
            if (!"ACTIVE".equals(account.status()) || !session.userId().equals(userId)
                    || session.revokedAt() != null || !session.expiresAt().isAfter(clock.instant()))
                throw new BadCredentialsException("Invalid session");
            var authorities = new ArrayList<SimpleGrantedAuthority>();
            store.roles(userId).stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).forEach(authorities::add);
            store.permissions(userId).stream().map(SimpleGrantedAuthority::new).forEach(authorities::add);
            return new JwtAuthenticationToken(jwt, authorities, userId.toString());
        } catch (IllegalArgumentException | java.util.NoSuchElementException e) {
            throw new BadCredentialsException("Invalid token subject");
        }
    }
}
