package com.vointika.identity.infrastructure.security;

import com.vointika.shared.web.security.RateLimitRule;
import com.vointika.shared.web.security.RateLimitRuleRegistrar;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Per-IP limits on identity's browser-public auth endpoints — the surface an
 * unauthenticated attacker can hammer (credential guessing on login/refresh,
 * email-bombing / SES cost amplification on the email routes). Generous for
 * humans, hostile to scripts. Per-account throttles additionally live in the
 * identity use cases (they need the request body's email).
 */
@Component
public class IdentityRateLimitRoutes implements RateLimitRuleRegistrar {

    @Override
    public List<RateLimitRule> rateLimitRules() {
        return List.of(
                new RateLimitRule(HttpMethod.POST, "/api/auth/login", 10, Duration.ofMinutes(1)),
                new RateLimitRule(HttpMethod.POST, "/api/auth/refresh", 60, Duration.ofMinutes(1)),
                new RateLimitRule(HttpMethod.POST, "/api/auth/register", 10, Duration.ofHours(1)),
                new RateLimitRule(HttpMethod.POST, "/api/auth/resend-verification", 5, Duration.ofHours(1)),
                new RateLimitRule(HttpMethod.POST, "/api/auth/request-password-reset", 5, Duration.ofHours(1)),
                // Token-consuming endpoints: guessing a 256-bit token is infeasible —
                // these caps bound junk DB load, not compromise.
                new RateLimitRule(HttpMethod.POST, "/api/auth/reset-password", 10, Duration.ofHours(1)),
                new RateLimitRule(HttpMethod.GET, "/api/auth/verify", 20, Duration.ofHours(1))
        );
    }
}
