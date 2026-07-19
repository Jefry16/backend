package com.vointika.shared.web.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class InternalApiSecretFilterTest {

    private static final String SECRET = "correct-internal-secret-value";
    private final InternalApiSecretFilter filter = new InternalApiSecretFilter(SECRET);

    private static MockHttpServletRequest internalRequest(String headerValue) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/internal/render-context/foo");
        if (headerValue != null) {
            req.addHeader(InternalApiSecretFilter.HEADER_NAME, headerValue);
        }
        return req;
    }

    @Test
    void correctSecretPassesThrough() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(internalRequest(SECRET), res, chain);

        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull(); // chain proceeded
    }

    @Test
    void wrongSecretSameLengthIs401() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(internalRequest("x".repeat(SECRET.length())), res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull(); // short-circuited
    }

    @Test
    void wrongSecretDifferentLengthIs401() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // A length very different from the real secret must still be rejected the
        // same way — the digest compare removes the length side-channel.
        filter.doFilter(internalRequest("short"), res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void missingHeaderIs401() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(internalRequest(null), res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void nonInternalPathPassesThroughWithoutSecret() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/tour-operators/x");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }
}
