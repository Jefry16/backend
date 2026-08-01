package com.vointika.shared.web.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

/**
 * Gates {@code /api/storefront/**} with a shared-secret header.
 *
 * <p>Runs before {@link JwtAuthenticationFilter}. Non-{@code /api/storefront/**}
 * paths pass through untouched. For matching paths, the filter compares
 * {@code X-Internal-Secret} against the configured secret using a constant-time
 * byte comparison; mismatch returns 401 with the standard error body and the
 * request is short-circuited (no downstream filters or handlers run).
 *
 * <p>Bound to {@code /api/storefront/**} so the public storefront routes (which
 * are also wired via {@link PublicRouteRegistrar} for JWT exemption) get
 * authenticated via shared secret instead. JWT auth does not apply on this
 * path family.
 */
public class InternalApiSecretFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Internal-Secret";
    private static final String PATH_PREFIX = "/api/storefront/";

    private final byte[] expectedDigest;

    public InternalApiSecretFilter(String sharedSecret) {
        if (sharedSecret == null || sharedSecret.isBlank()) {
            throw new IllegalStateException(
                    "app.internal.shared-secret must be configured (set APP_INTERNAL_SHARED_SECRET)");
        }
        this.expectedDigest = sha256(sharedSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith(PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // SECURITY: never log the received secret, the expected secret, or the
        // X-Internal-Secret header value in any log line or response body. A
        // generic exception handler upstream must also avoid reflecting request
        // headers, since this filter sits before @ControllerAdvice can reach.
        String provided = request.getHeader(HEADER_NAME);
        // Compare fixed-width SHA-256 digests, not the raw bytes: MessageDigest.isEqual
        // returns early when the two arrays differ in length, which would leak the
        // secret's byte length via response timing. Hashing both sides first makes
        // every comparison 32 bytes regardless of the provided value's length, so the
        // only thing the constant-time compare can reveal is match/no-match.
        if (provided == null || !MessageDigest.isEqual(
                expectedDigest, sha256(provided.getBytes(StandardCharsets.UTF_8)))) {
            writeUnauthorized(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a JVM-required algorithm; its absence is a fatal environment fault.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        // Same shape as ApiErrorResponse but written directly to avoid a
        // dependency cycle into the controller advice (we're a filter).
        response.getWriter().write(
                "{\"status\":401,\"error\":\"Unauthorized\","
                        + "\"message\":\"Invalid internal API credentials\","
                        + "\"timestamp\":\"" + Instant.now() + "\"}");
    }
}
