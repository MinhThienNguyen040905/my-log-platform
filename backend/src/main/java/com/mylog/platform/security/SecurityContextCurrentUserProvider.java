package com.mylog.platform.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
final class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Optional<CurrentUser> findCurrent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        try {
            UUID userId = UUID.fromString(authentication.getName());
            var authorities = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toUnmodifiableSet());
            return Optional.of(new CurrentUser(userId, authorities));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
