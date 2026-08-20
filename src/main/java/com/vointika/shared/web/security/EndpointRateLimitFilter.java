package com.vointika.shared.web.security;

import com.vointika.shared.port.RateLimiterPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.IOException;
import java.util.List;

/**
 * Layer A of rate limiting (see {@code PATTERNS.md}): per-IP caps on the
 * endpoints an unauthenticated attacker can hammer — credential guessing on
 * login/refresh, email-bombing / SES cost amplification on the email routes, and
 * account-provisioning on invitation accept. Per-ACCOUNT throttles live in the
 * use cases (they need the request body); the coarse per-user cap is
 * {@link ApiRateLimitFilter}. This filter only needs the client IP and the path.
 *
 * <p>Rules are declared per-context via {@link RateLimitRuleRegistrar} and matched
 * by Spring {@link PathPattern}, so a path-variable route
 * ({@code /api/invitations/*&#47;accept}) is covered — and the counter keys on the
 * matched <b>pattern</b>, not the concrete URI, so rotating the variable shares one
 * bucket. First matching rule wins.
 *
 * <p>The limiter is resolved lazily via {@link ObjectProvider}: with no
 * {@code RateLimiterPort} bean (sliced tests) the filter passes everything
 * through, and the Redis implementation fails open when the store is down — rate
 * limiting never breaks a request. The 429 itself is {@link RateLimitRefusal}, shared
 * with {@link ApiRateLimitFilter} so the two cannot answer the same condition
 * differently.
 *
 * <p>{@code getRemoteAddr()} is the client IP only when the app terminates the
 * connection; behind a proxy/LB, configure {@code server.forward-headers-strategy}
 * so the servlet API resolves {@code X-Forwarded-For} before this filter reads it.
 */
@Component
public class EndpointRateLimitFilter extends OncePerRequestFilter {

    private record CompiledRule(HttpMethod method, String patternText, PathPattern pattern,
                                int limit, java.time.Duration window) {}

    private final List<CompiledRule> rules;
    private final ObjectProvider<RateLimiterPort> rateLimiterProvider;
    private final boolean enabled;

    public EndpointRateLimitFilter(List<RateLimitRuleRegistrar> registrars,
                                   ObjectProvider<RateLimiterPort> rateLimiterProvider,
                                   @Value("${app.rate-limit.enabled:true}") boolean enabled) {
        PathPatternParser parser = new PathPatternParser();
        this.rules = registrars.stream()
                .flatMap(r -> r.rateLimitRules().stream())
                .map(r -> new CompiledRule(r.method(), r.pathPattern(),
                        parser.parse(r.pathPattern()), r.limit(), r.window()))
                .toList();
        this.rateLimiterProvider = rateLimiterProvider;
        this.enabled = enabled;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        CompiledRule rule = enabled ? match(request) : null;
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }
        RateLimiterPort limiter = rateLimiterProvider.getIfAvailable();
        if (limiter == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Key on the matched PATTERN, never the concrete URI — otherwise a
        // path-variable route buckets per value and never limits.
        String key = "rl:ip:" + rule.method().name() + ":" + rule.patternText()
                + ":" + request.getRemoteAddr();
        if (!limiter.tryAcquire(key, rule.limit(), rule.window())) {
            RateLimitRefusal.write(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private CompiledRule match(HttpServletRequest request) {
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        PathContainer path = PathContainer.parsePath(request.getRequestURI());
        for (CompiledRule rule : rules) {
            if (rule.method().equals(method) && rule.pattern().matches(path)) {
                return rule;
            }
        }
        return null;
    }
}
