package com.vointika.storefront.infrastructure.security;

import com.vointika.shared.web.security.RateLimitRule;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StorefrontRateLimitRoutesTest {

    /**
     * The numbers are asserted, not just the route: a storefront password is one
     * shared value with no account to lock and no owner to notify, so the limit
     * is the only thing between it and an offline-speed guess. Raising it is a
     * decision, and this is where it gets made.
     */
    @Test
    void throttlesThePasswordSubmissionAndNothingElse() {
        List<RateLimitRule> rules = new StorefrontRateLimitRoutes().rateLimitRules();

        assertThat(rules).containsExactly(
                new RateLimitRule(HttpMethod.POST, "/password", 20, Duration.ofHours(1)));
    }
}
