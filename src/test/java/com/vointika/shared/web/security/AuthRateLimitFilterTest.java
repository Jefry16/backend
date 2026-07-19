package com.vointika.shared.web.security;

import com.vointika.shared.port.RateLimiterPort;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthRateLimitFilterTest {

    @Mock private ObjectProvider<RateLimiterPort> rateLimiterProvider;
    @Mock private RateLimiterPort rateLimiter;
    @Mock private FilterChain chain;

    private AuthRateLimitFilter enabledFilter() {
        return new AuthRateLimitFilter(rateLimiterProvider, true);
    }

    @Test
    void nonMatchingPathPassesThroughWithoutConsultingLimiter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/experiences");
        MockHttpServletResponse response = new MockHttpServletResponse();

        enabledFilter().doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(rateLimiterProvider, rateLimiter);
    }

    @Test
    void getOnMatchingPathPassesThroughWithoutConsultingLimiter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        enabledFilter().doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(rateLimiterProvider, rateLimiter);
    }

    @Test
    void passesThroughWhenNoLimiterBeanIsAvailable() throws Exception {
        when(rateLimiterProvider.getIfAvailable()).thenReturn(null);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        enabledFilter().doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void passesThroughWhenDisabledByProperty() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new AuthRateLimitFilter(rateLimiterProvider, false).doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(rateLimiterProvider, rateLimiter);
    }

    @Test
    void denialWrites429JsonAndStopsTheChain() throws Exception {
        when(rateLimiterProvider.getIfAvailable()).thenReturn(rateLimiter);
        when(rateLimiter.tryAcquire("rl:ip:/api/auth/login:127.0.0.1", 10, Duration.ofMinutes(1)))
                .thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        enabledFilter().doFilter(request, response, chain);

        assertEquals(429, response.getStatus());
        assertTrue(response.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE));
        assertTrue(response.getContentAsString().contains("Too Many Requests"));
        verifyNoInteractions(chain);
    }

    @Test
    void resetPasswordPostIsLimited() throws Exception {
        when(rateLimiterProvider.getIfAvailable()).thenReturn(rateLimiter);
        when(rateLimiter.tryAcquire("rl:ip:/api/auth/reset-password:127.0.0.1", 10, Duration.ofHours(1)))
                .thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/reset-password");
        MockHttpServletResponse response = new MockHttpServletResponse();

        enabledFilter().doFilter(request, response, chain);

        assertEquals(429, response.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void verifyGetIsLimited() throws Exception {
        when(rateLimiterProvider.getIfAvailable()).thenReturn(rateLimiter);
        when(rateLimiter.tryAcquire("rl:ip:/api/auth/verify:127.0.0.1", 20, Duration.ofHours(1)))
                .thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/verify");
        MockHttpServletResponse response = new MockHttpServletResponse();

        enabledFilter().doFilter(request, response, chain);

        assertEquals(429, response.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void allowedRequestContinuesTheChain() throws Exception {
        when(rateLimiterProvider.getIfAvailable()).thenReturn(rateLimiter);
        when(rateLimiter.tryAcquire("rl:ip:/api/auth/login:127.0.0.1", 10, Duration.ofMinutes(1)))
                .thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        enabledFilter().doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }
}
