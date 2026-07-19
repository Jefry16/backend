package com.vointika.shared.web.security;

import com.vointika.shared.port.RateLimiterPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Per-IP rate limits on the browser-public auth endpoints (§7.9) — the
 * surface an unauthenticated attacker can hammer: credential guessing on
 * login/refresh, and email-bombing / SES cost amplification on the
 * email-sending routes. Per-ACCOUNT throttles live in the identity use
 * cases (they need the request body's email); this filter only needs the
 * client IP and the path.
 *
 * <p>Limits are generous for humans, hostile to scripts. 429 responses use
 * the §6.1 error shape, written directly (same pattern as
 * {@code RequestSizeLimitFilter}).
 *
 * <p>The limiter is resolved lazily via {@link ObjectProvider}: in sliced
 * test contexts (no Redis, no {@code RateLimiterPort} bean) the filter
 * passes everything through, and the Redis implementation itself fails
 * open when the store is down — rate limiting never breaks auth.
 *
 * <p>{@code getRemoteAddr()} is the client IP only when the app terminates
 * the connection; behind a proxy/LB, configure
 * {@code server.forward-headers-strategy} so the servlet API resolves
 * {@code X-Forwarded-For} before this filter reads it.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private record Rule(int limit, Duration window) {
    }

    /** Method-qualified rules; key is "METHOD /path". */
    private static final Map<String, Rule> RULES = Map.of(
            "POST /api/auth/login", new Rule(10, Duration.ofMinutes(1)),
            "POST /api/auth/refresh", new Rule(60, Duration.ofMinutes(1)),
            "POST /api/auth/register", new Rule(10, Duration.ofHours(1)),
            "POST /api/auth/resend-verification", new Rule(5, Duration.ofHours(1)),
            "POST /api/auth/request-password-reset", new Rule(5, Duration.ofHours(1)),
            // Token-consuming endpoints: guessing a 256-bit token is
            // infeasible — these caps bound junk DB load, not compromise.
            "POST /api/auth/reset-password", new Rule(10, Duration.ofHours(1)),
            "GET /api/auth/verify", new Rule(20, Duration.ofHours(1))
    );

    private final ObjectProvider<RateLimiterPort> rateLimiterProvider;
    private final boolean enabled;

    public AuthRateLimitFilter(ObjectProvider<RateLimiterPort> rateLimiterProvider,
                               @Value("${app.rate-limit.enabled:true}") boolean enabled) {
        this.rateLimiterProvider = rateLimiterProvider;
        this.enabled = enabled;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        Rule rule = RULES.get(request.getMethod() + " " + request.getRequestURI());
        if (rule == null || !enabled) {
            filterChain.doFilter(request, response);
            return;
        }
        RateLimiterPort limiter = rateLimiterProvider.getIfAvailable();
        if (limiter == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = "rl:ip:" + request.getRequestURI() + ":" + request.getRemoteAddr();
        if (!limiter.tryAcquire(key, rule.limit(), rule.window())) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            // ASCII-only message: this body bypasses Jackson, so it must not
            // depend on the container's default writer charset.
            String body = "{\"status\":429,\"error\":\"Too Many Requests\","
                    + "\"message\":\"Too many requests, try again later\","
                    + "\"timestamp\":\"" + Instant.now() + "\"}";
            response.getWriter().write(body);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
