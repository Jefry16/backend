package com.vointika.shared.web.security;

import com.vointika.identity.infrastructure.security.IdentityRateLimitRoutes;
import com.vointika.shared.port.RateLimiterPort;
import com.vointika.touroperator.infrastructure.security.TourOperatorRateLimitRoutes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Proves the pattern is <b>repeatable</b>: a bounded context declares a
 * {@link RateLimitRuleRegistrar} bean and the shared {@link EndpointRateLimitFilter}
 * enforces it with <b>zero changes to shared/filter code</b> — Spring collects
 * every registrar via {@code List<RateLimitRuleRegistrar>} injection. Verified by
 * booting a real (DB-free) context with the two shipped registrars PLUS a synthetic
 * "future context" registrar, and confirming a rule from each is enforced.
 */
class RateLimitRegistrarWiringTest {

    @Configuration
    static class Beans {
        /** Deny everything, so any *matched* request becomes 429 — proving the rule was collected + matched. */
        @Bean RateLimiterPort limiter() {
            return (key, limit, window) -> false;
        }
        @Bean RateLimitRuleRegistrar identityRoutes() { return new IdentityRateLimitRoutes(); }
        @Bean RateLimitRuleRegistrar tourOperatorRoutes() { return new TourOperatorRateLimitRoutes(); }
        /** A brand-new context added LATER — just a @Bean registrar, nothing else. */
        @Bean RateLimitRuleRegistrar futureContextRoutes() {
            return () -> List.of(new RateLimitRule(
                    HttpMethod.POST, "/api/future/*/thing", 5, Duration.ofMinutes(1)));
        }
        @Bean EndpointRateLimitFilter filter(List<RateLimitRuleRegistrar> registrars,
                                             ObjectProvider<RateLimiterPort> limiter) {
            return new EndpointRateLimitFilter(registrars, limiter, true);
        }
    }

    private final ApplicationContextRunner context =
            new ApplicationContextRunner().withUserConfiguration(Beans.class);

    @Test
    void filterAutoCollectsRulesFromEveryContextsRegistrar_includingOneAddedLater() {
        context.run(ctx -> {
            EndpointRateLimitFilter filter = ctx.getBean(EndpointRateLimitFilter.class);

            // One rule from each of the THREE independently-declared registrars.
            assertEquals(429, statusFor(filter, "POST", "/api/auth/login"),
                    "identity registrar's rule enforced");
            assertEquals(429, statusFor(filter, "POST", "/api/invitations/tok/accept"),
                    "touroperator registrar's rule enforced");
            assertEquals(429, statusFor(filter, "POST", "/api/future/xyz/thing"),
                    "a future context's registrar is picked up with no filter change");

            // A path no registrar declared still passes through untouched.
            MockFilterChain chain = new MockFilterChain();
            MockHttpServletResponse passed = doFilter(filter, "POST", "/api/unregistered", chain);
            assertEquals(200, passed.getStatus());
            assertNotNull(chain.getRequest(), "unregistered path continued down the chain");
        });
    }

    private static int statusFor(EndpointRateLimitFilter filter, String method, String uri) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = doFilter(filter, method, uri, chain);
        assertNull(chain.getRequest(), "a limited request must not continue the chain");
        return response.getStatus();
    }

    private static MockHttpServletResponse doFilter(EndpointRateLimitFilter filter, String method,
                                                    String uri, MockFilterChain chain) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }
}
