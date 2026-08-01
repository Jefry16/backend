package com.vointika.shared.web.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class StorefrontApiSecretFilterTest {

    private static final String SECRET = "correct-storefront-secret-value";
    private final StorefrontApiSecretFilter filter = new StorefrontApiSecretFilter(SECRET);

    private static MockHttpServletRequest storefrontRequest(String headerValue) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/storefront/render-context/foo");
        if (headerValue != null) {
            req.addHeader(StorefrontApiSecretFilter.HEADER_NAME, headerValue);
        }
        return req;
    }

    /**
     * The header name is a wire contract: the storefront BFF sends this exact
     * string on every call. Every other test here reaches for the constant, so
     * changing its value used to break nothing — renaming it from
     * {@code X-Internal-Secret} left the whole suite green, and the failure would
     * have been every storefront request 401ing in production.
     */
    @Test
    void theHeaderNameIsPinnedBecauseTheStorefrontSendsIt() {
        assertThat(StorefrontApiSecretFilter.HEADER_NAME)
                .withFailMessage(
                        "The shared-secret header is named '%s'. Changing it is a coordinated "
                                + "change with the storefront BFF, which sends it on every call — "
                                + "update src/lib/internalApi.ts in the storefront repo in the same "
                                + "breath, or every request 401s.",
                        StorefrontApiSecretFilter.HEADER_NAME)
                .isEqualTo("X-Storefront-Secret");
    }

    @Test
    void correctSecretPassesThrough() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(storefrontRequest(SECRET), res, chain);

        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull(); // chain proceeded
    }

    @Test
    void wrongSecretSameLengthIs401() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(storefrontRequest("x".repeat(SECRET.length())), res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull(); // short-circuited
    }

    @Test
    void wrongSecretDifferentLengthIs401() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // A length very different from the real secret must still be rejected the
        // same way — the digest compare removes the length side-channel.
        filter.doFilter(storefrontRequest("short"), res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void missingHeaderIs401() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(storefrontRequest(null), res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void nonStorefrontPathPassesThroughWithoutSecret() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/tour-operators/x");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }
}
