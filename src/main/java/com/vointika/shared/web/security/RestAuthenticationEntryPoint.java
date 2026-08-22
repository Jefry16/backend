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
import java.util.List;
import java.util.Optional;

/**
 * Returns 401 with an ApiErrorResponse-shaped body when a request hits a protected
 * endpoint without valid authentication. Overrides Spring Security's default 403.
 *
 * <p><b>Unless a context claims the request first.</b> A
 * {@link UnauthenticatedRequestPolicy} can answer that this surface is public, in
 * which case a 401 is not merely unhelpful but untrue — nobody could have
 * authenticated there. See that interface for the case it was written for.
 *
 * <p>The body is hand-written rather than serialized because this runs in the
 * filter chain, before MVC and its message converters exist. That is also why the
 * message is escaped here: it arrives from a context, and a stray quote would
 * otherwise produce a body no client can parse.
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final List<UnauthenticatedRequestPolicy> policies;

    public RestAuthenticationEntryPoint(List<UnauthenticatedRequestPolicy> policies) {
        this.policies = List.copyOf(policies);
    }

    @Override
    public void commence(@NonNull HttpServletRequest request,
                         HttpServletResponse response,
                         @NonNull AuthenticationException authException) throws IOException {
        Optional<String> notFound = policies.stream()
                .flatMap(policy -> policy.notFoundMessage(request).stream())
                .findFirst();

        HttpStatus status = notFound.isPresent() ? HttpStatus.NOT_FOUND : HttpStatus.UNAUTHORIZED;
        String message = notFound.orElse("Authentication required");

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"status\":" + status.value()
                + ",\"error\":\"" + status.getReasonPhrase() + "\""
                + ",\"message\":\"" + escape(message) + "\""
                + ",\"timestamp\":\"" + Instant.now() + "\"}");
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
