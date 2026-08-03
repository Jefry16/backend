package com.vointika.storefront.infrastructure.security;

import com.vointika.shared.web.security.RateLimitRule;
import com.vointika.shared.web.security.RateLimitRuleRegistrar;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * The storefront password is one shared value per store, so unlimited attempts
 * make it brute-forceable in a way a per-account credential is not — there is no
 * account to lock and no owner to notify. Twenty an hour per IP leaves a visitor
 * who mistyped it plenty of room and makes a search of any real password space
 * hopeless.
 *
 * <p>Only the submission is limited: {@code GET /password} hands out nothing an
 * attacker cannot get by asking for {@code /}.
 */
@Component
public class StorefrontRateLimitRoutes implements RateLimitRuleRegistrar {

    @Override
    public List<RateLimitRule> rateLimitRules() {
        return List.of(
                new RateLimitRule(HttpMethod.POST, "/password", 20, Duration.ofHours(1))
        );
    }
}
