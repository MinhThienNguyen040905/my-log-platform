package com.mylog.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class ActiveAccountFilter extends OncePerRequestFilter {

    private final AccountStatusVerifier accountStatusVerifier;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    public ActiveAccountFilter(
            AccountStatusVerifier accountStatusVerifier,
            RestAuthenticationEntryPoint authenticationEntryPoint) {
        this.accountStatusVerifier = accountStatusVerifier;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                UUID userId = UUID.fromString(authentication.getName());
                if (!accountStatusVerifier.isActive(userId)) {
                    SecurityContextHolder.clearContext();
                    authenticationEntryPoint.commence(
                            request,
                            response,
                            new org.springframework.security.authentication.DisabledException(
                                    "Account is unavailable"));
                    return;
                }
            } catch (IllegalArgumentException exception) {
                SecurityContextHolder.clearContext();
                authenticationEntryPoint.commence(
                        request,
                        response,
                        new org.springframework.security.authentication.BadCredentialsException(
                                "Invalid token subject"));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
