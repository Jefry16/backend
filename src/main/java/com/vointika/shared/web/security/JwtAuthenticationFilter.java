package com.vointika.shared.web.security;

import com.vointika.shared.port.AccessTokenValidatorPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String USER_ID_MDC_KEY = "userId";

    private final AccessTokenValidatorPort accessTokenValidator;

    public JwtAuthenticationFilter(AccessTokenValidatorPort accessTokenValidator) {
        this.accessTokenValidator = accessTokenValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!accessTokenValidator.isValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = accessTokenValidator.extractUserId(token);

        // Reject tokens whose subject isn't a UUID — caller will be unauthenticated,
        // the AuthenticationEntryPoint returns 401.
        UUID callerId;
        try {
            callerId = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            filterChain.doFilter(request, response);
            return;
        }

        // The principal is the UUID, not its text. Parsing here and storing the
        // String meant every controller re-parsed it — 134 copies of
        // UUID.fromString across 33 controllers, each able to throw on input this
        // filter had already proven valid.
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        callerId,
                        null,
                        Collections.emptyList()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        MDC.put(USER_ID_MDC_KEY, userId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(USER_ID_MDC_KEY);
        }
    }
}