package com.vointika.identity.infrastructure.security;

import com.vointika.shared.web.security.RateLimitRule;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentityRateLimitRoutesTest {

    @Test
    void declaresPerIpLimitsForTheSevenPublicAuthEndpoints() {
        List<RateLimitRule> rules = new IdentityRateLimitRoutes().rateLimitRules();

        assertEquals(7, rules.size());
        assertTrue(rules.stream().anyMatch(r ->
                r.method() == HttpMethod.POST && r.pathPattern().equals("/api/auth/login") && r.limit() == 10));
        assertTrue(rules.stream().anyMatch(r ->
                r.method() == HttpMethod.GET && r.pathPattern().equals("/api/auth/verify")));
    }
}
