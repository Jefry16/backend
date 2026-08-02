package com.vointika.shared.web.security;

import com.vointika.shared.port.RateLimiterPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * Coarse per-user safety cap on the authenticated admin API (§7.9, third
 * layer). Not an abuse defense — callers here are authenticated members —
 * but a backstop against runaway scripts and buggy client loops. The limit
 * is far above any human/SPA usage; hitting it means something is wrong
 * with the caller.
 *
 * <p>Keys on the JWT subject (per user, across all their operators), so one
 * misbehaving client can't degrade the shared instance for everyone. The
 * API domain is deliberately un-proxied (grey-cloud, for the SameSite
 * cookie), so this app-side filter is the only place such a cap can live.
 *
 * <p>Scoped to JWT-authenticated requests only: unauthenticated and public
 * routes pass through untouched. Same fail-open posture and 429 shape as
 * {@link EndpointRateLimitFilter}.
 */
public class ApiRateLimitFilter extends OncePerRequestFilter {

    static final int LIMIT = 300;
    static final Duration WINDOW = Duration.ofMinutes(1);

    private final ObjectProvider<RateLimiterPort> rateLimiterProvider;
    private final boolean enabled;

    public ApiRateLimitFilter(ObjectProvider<RateLimiterPort> rateLimiterProvider, boolean enabled) {
        this.rateLimiterProvider = rateLimiterProvider;
        this.enabled = enabled;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof UsernamePasswordAuthenticationToken)
                || !(auth.getPrincipal() instanceof String userId)) {
            filterChain.doFilter(request, response);
            return;
        }
        RateLimiterPort limiter = rateLimiterProvider.getIfAvailable();
        if (limiter == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!limiter.tryAcquire("rl:api:user:" + userId, LIMIT, WINDOW)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            // ASCII-only: bypasses Jackson (see EndpointRateLimitFilter).
            String body = "{\"status\":429,\"error\":\"Too Many Requests\","
                    + "\"message\":\"Too many requests, try again later\","
                    + "\"timestamp\":\"" + Instant.now() + "\"}";
            response.getWriter().write(body);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
