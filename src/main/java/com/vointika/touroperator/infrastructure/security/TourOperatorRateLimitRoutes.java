package com.vointika.touroperator.infrastructure.security;

import com.vointika.shared.web.security.RateLimitRule;
import com.vointika.shared.web.security.RateLimitRuleRegistrar;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Per-IP limits on the public invitation endpoints (the token is the capability,
 * so there's no authenticated principal to key on). {@code accept} provisions an
 * account + issues a session, so it's the tighter cap; {@code preview} is a cheap
 * read. The {@code *} matches the token path segment — the counter keys on the
 * pattern, so rotating tokens shares one bucket. Values are starting points, tunable.
 */
@Component
public class TourOperatorRateLimitRoutes implements RateLimitRuleRegistrar {

    @Override
    public List<RateLimitRule> rateLimitRules() {
        return List.of(
                new RateLimitRule(HttpMethod.POST, "/api/invitations/*/accept", 10, Duration.ofHours(1)),
                new RateLimitRule(HttpMethod.GET, "/api/invitations/*/preview", 60, Duration.ofHours(1))
        );
    }
}
