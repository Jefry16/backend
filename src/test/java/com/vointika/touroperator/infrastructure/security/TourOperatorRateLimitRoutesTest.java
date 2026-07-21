package com.vointika.touroperator.infrastructure.security;

import com.vointika.shared.web.security.RateLimitRule;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TourOperatorRateLimitRoutesTest {

    @Test
    void declaresPerIpLimitsForBothPublicInvitationEndpoints() {
        List<RateLimitRule> rules = new TourOperatorRateLimitRoutes().rateLimitRules();

        assertTrue(rules.stream().anyMatch(r ->
                r.method() == HttpMethod.POST && r.pathPattern().equals("/api/invitations/*/accept")));
        assertTrue(rules.stream().anyMatch(r ->
                r.method() == HttpMethod.GET && r.pathPattern().equals("/api/invitations/*/preview")));
        assertEquals(2, rules.size());
    }
}
