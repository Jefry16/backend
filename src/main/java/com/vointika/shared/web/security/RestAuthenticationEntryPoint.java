package com.vointika.shared.web.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.time.Instant;

/**
 * Returns 401 with an ApiErrorResponse-shaped body when a request hits a protected
 * endpoint without valid authentication. Overrides Spring Security's default 403.
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(@NonNull HttpServletRequest request,
                         HttpServletResponse response,
                         @NonNull AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String body = "{\"status\":401,\"error\":\"Unauthorized\","
                + "\"message\":\"Authentication required\","
                + "\"timestamp\":\"" + Instant.now() + "\"}";
        response.getWriter().write(body);
    }
}
