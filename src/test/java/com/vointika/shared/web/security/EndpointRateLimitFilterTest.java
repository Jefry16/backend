package com.vointika.shared.web.security;

import com.vointika.shared.port.RateLimiterPort;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EndpointRateLimitFilterTest {

    @Mock private ObjectProvider<RateLimiterPort> rateLimiterProvider;
    @Mock private RateLimiterPort rateLimiter;
    @Mock private FilterChain chain;

    // A registrar mixing exact-path (identity) and path-variable (invitation) rules.
    private static final List<RateLimitRuleRegistrar> REGISTRARS = List.of(() -> List.of(
            new RateLimitRule(HttpMethod.POST, "/api/auth/login", 10, Duration.ofMinutes(1)),
            new RateLimitRule(HttpMethod.GET, "/api/auth/verify", 20, Duration.ofHours(1)),
            new RateLimitRule(HttpMethod.POST, "/api/invitations/*/accept", 10, Duration.ofHours(1)),
            new RateLimitRule(HttpMethod.GET, "/api/invitations/*/preview", 60, Duration.ofHours(1))));

    private EndpointRateLimitFilter enabledFilter() {
        return new EndpointRateLimitFilter(REGISTRARS, rateLimiterProvider, true);
    }

    private static MockHttpServletRequest req(String method, String uri) {
        return new MockHttpServletRequest(method, uri); // getRemoteAddr() defaults to 127.0.0.1
    }

    @Test
    void nonMatchingPathPassesThroughWithoutConsultingLimiter() throws Exception {
        MockHttpServletRequest request = req("POST", "/api/experiences");
        MockHttpServletResponse response = new MockHttpServletResponse();
        enabledFilter().doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(rateLimiterProvider, rateLimiter);
    }

    @Test
    void methodMismatchPassesThroughWithoutConsultingLimiter() throws Exception {
        // login is POST-only; a GET on the same path must not match.
        MockHttpServletRequest request = req("GET", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        enabledFilter().doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(rateLimiterProvider, rateLimiter);
    }

    @Test
    void passesThroughWhenNoLimiterBeanIsAvailable() throws Exception {
        when(rateLimiterProvider.getIfAvailable()).thenReturn(null);
        MockHttpServletRequest request = req("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        enabledFilter().doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void passesThroughWhenDisabledByProperty() throws Exception {
        MockHttpServletRequest request = req("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new EndpointRateLimitFilter(REGISTRARS, rateLimiterProvider, false)
                .doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(rateLimiterProvider, rateLimiter);
    }

    @Test
    void denialWrites429JsonAndStopsTheChain() throws Exception {
        when(rateLimiterProvider.getIfAvailable()).thenReturn(rateLimiter);
        when(rateLimiter.tryAcquire("rl:ip:POST:/api/auth/login:127.0.0.1", 10, Duration.ofMinutes(1)))
                .thenReturn(false);
        MockHttpServletRequest request = req("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        enabledFilter().doFilter(request, response, chain);

        assertEquals(429, response.getStatus());
        assertTrue(response.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE));
        // Was contains("Too Many Requests"), which matches the `error` field and so passes
        // against almost any 429 — including one whose message had drifted.
        assertEquals("{\"status\":429,\"error\":\"Too Many Requests\","
                        + "\"message\":\"Too many requests, try again later\","
                        + "\"timestamp\":\"<stamped>\"}",
                response.getContentAsString()
                        .replaceAll("\"timestamp\":\"[^\"]+\"", "\"timestamp\":\"<stamped>\""));
        verifyNoInteractions(chain);
    }

    @Test
    void exactPathRulesAreMethodQualified() throws Exception {
        when(rateLimiterProvider.getIfAvailable()).thenReturn(rateLimiter);
        when(rateLimiter.tryAcquire("rl:ip:GET:/api/auth/verify:127.0.0.1", 20, Duration.ofHours(1)))
                .thenReturn(false);
        MockHttpServletRequest request = req("GET", "/api/auth/verify");
        MockHttpServletResponse response = new MockHttpServletResponse();
        enabledFilter().doFilter(request, response, chain);
        assertEquals(429, response.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void allowedRequestContinuesTheChain() throws Exception {
        when(rateLimiterProvider.getIfAvailable()).thenReturn(rateLimiter);
        when(rateLimiter.tryAcquire("rl:ip:POST:/api/auth/login:127.0.0.1", 10, Duration.ofMinutes(1)))
                .thenReturn(true);
        MockHttpServletRequest request = req("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        enabledFilter().doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    // The point of the rewrite: a path-variable route IS limited, and rotating the
    // variable does NOT dodge the bucket — both tokens hit the same pattern key.
    @Test
    void pathVariableRouteIsLimitedAndRotatingTheTokenSharesOneBucket() throws Exception {
        String bucket = "rl:ip:POST:/api/invitations/*/accept:127.0.0.1";
        when(rateLimiterProvider.getIfAvailable()).thenReturn(rateLimiter);
        when(rateLimiter.tryAcquire(bucket, 10, Duration.ofHours(1))).thenReturn(false);

        MockHttpServletResponse r1 = new MockHttpServletResponse();
        enabledFilter().doFilter(req("POST", "/api/invitations/token-AAA/accept"), r1, chain);
        MockHttpServletResponse r2 = new MockHttpServletResponse();
        enabledFilter().doFilter(req("POST", "/api/invitations/token-BBB/accept"), r2, chain);

        assertEquals(429, r1.getStatus());
        assertEquals(429, r2.getStatus());
        // Both distinct tokens consulted the SAME pattern-keyed bucket, twice.
        verify(rateLimiter, times(2)).tryAcquire(bucket, 10, Duration.ofHours(1));
        verifyNoInteractions(chain);
    }

    @Test
    void previewPathVariableRouteMatches() throws Exception {
        String bucket = "rl:ip:GET:/api/invitations/*/preview:127.0.0.1";
        when(rateLimiterProvider.getIfAvailable()).thenReturn(rateLimiter);
        when(rateLimiter.tryAcquire(bucket, 60, Duration.ofHours(1))).thenReturn(true);
        MockHttpServletRequest request = req("GET", "/api/invitations/any-token/preview");
        MockHttpServletResponse response = new MockHttpServletResponse();
        enabledFilter().doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verify(rateLimiter).tryAcquire(bucket, 60, Duration.ofHours(1));
    }
}
