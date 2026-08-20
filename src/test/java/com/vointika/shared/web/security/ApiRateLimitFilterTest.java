package com.vointika.shared.web.security;

import com.vointika.shared.port.RateLimiterPort;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ApiRateLimitFilterTest {

    /** A UUID, not its text: the filter keys on the parsed principal the JWT filter stores. */
    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Mock private ObjectProvider<RateLimiterPort> rateLimiterProvider;
    @Mock private RateLimiterPort rateLimiter;
    @Mock private FilterChain chain;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null, Collections.emptyList()));
    }

    private ApiRateLimitFilter enabledFilter() {
        return new ApiRateLimitFilter(rateLimiterProvider, true);
    }

    @Test
    void unauthenticatedRequestPassesThroughWithoutConsultingLimiter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/experiences");
        MockHttpServletResponse response = new MockHttpServletResponse();

        enabledFilter().doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(rateLimiterProvider, rateLimiter);
    }

    @Test
    void passesThroughWhenDisabledByProperty() throws Exception {
        authenticate();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/experiences");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new ApiRateLimitFilter(rateLimiterProvider, false).doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(rateLimiterProvider, rateLimiter);
    }

    @Test
    void passesThroughWhenNoLimiterBeanIsAvailable() throws Exception {
        authenticate();
        when(rateLimiterProvider.getIfAvailable()).thenReturn(null);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/experiences");
        MockHttpServletResponse response = new MockHttpServletResponse();

        enabledFilter().doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void allowedAuthenticatedRequestContinuesTheChain() throws Exception {
        authenticate();
        when(rateLimiterProvider.getIfAvailable()).thenReturn(rateLimiter);
        when(rateLimiter.tryAcquire("rl:api:user:" + USER_ID,
                ApiRateLimitFilter.LIMIT, ApiRateLimitFilter.WINDOW)).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/experiences");
        MockHttpServletResponse response = new MockHttpServletResponse();

        enabledFilter().doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void deniedAuthenticatedRequestGets429AndChainStops() throws Exception {
        authenticate();
        when(rateLimiterProvider.getIfAvailable()).thenReturn(rateLimiter);
        when(rateLimiter.tryAcquire("rl:api:user:" + USER_ID,
                ApiRateLimitFilter.LIMIT, ApiRateLimitFilter.WINDOW)).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/experiences");
        MockHttpServletResponse response = new MockHttpServletResponse();

        enabledFilter().doFilter(request, response, chain);

        assertEquals(429, response.getStatus());
        // The body was asserted nowhere until this — only the status was, which is how
        // both filters could carry the same nine hand-written lines and drift apart
        // unnoticed. Both answer through RateLimitRefusal now; this pins the bytes.
        assertEquals("{\"status\":429,\"error\":\"Too Many Requests\","
                        + "\"message\":\"Too many requests, try again later\","
                        + "\"timestamp\":\"<stamped>\"}",
                response.getContentAsString()
                        .replaceAll("\"timestamp\":\"[^\"]+\"", "\"timestamp\":\"<stamped>\""));
        verifyNoInteractions(chain);
    }
}
