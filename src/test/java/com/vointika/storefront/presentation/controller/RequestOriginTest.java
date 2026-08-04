package com.vointika.storefront.presentation.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The origin ends up in a {@code <link rel="canonical">}, so getting the port
 * wrong is not cosmetic: a canonical pointing at a URL the site does not serve is
 * worse than none at all.
 */
class RequestOriginTest {

    @Test
    void keepsANonDefaultPortBecauseTheSiteIsOnlyReachableThere() {
        assertThat(RequestOrigin.of(request("http", "acme.localhost", 8080)))
                .isEqualTo("http://acme.localhost:8080");
    }

    @Test
    void dropsTheDefaultPortForEachScheme() {
        assertThat(RequestOrigin.of(request("http", "acme.example.com", 80)))
                .isEqualTo("http://acme.example.com");
        assertThat(RequestOrigin.of(request("https", "acme.example.com", 443)))
                .isEqualTo("https://acme.example.com");
    }

    /**
     * 80 is only <em>http</em>'s default. On https it is a real port and dropping
     * it would emit an address the site does not answer on.
     */
    @Test
    void aDefaultPortOfTheOtherSchemeIsStillAPort() {
        assertThat(RequestOrigin.of(request("https", "acme.example.com", 80)))
                .isEqualTo("https://acme.example.com:80");
        assertThat(RequestOrigin.of(request("http", "acme.example.com", 443)))
                .isEqualTo("http://acme.example.com:443");
    }

    /**
     * The scheme follows the request rather than configuration, so that behind a
     * proxy {@code ForwardedHeaderFilter} can make it honour
     * {@code X-Forwarded-Proto} — the same reason the tenant comes from
     * {@code getServerName()}.
     */
    @Test
    void theSchemeFollowsTheRequest() {
        assertThat(RequestOrigin.of(request("https", "acme.example.com", 443)))
                .startsWith("https://");
    }

    private static MockHttpServletRequest request(String scheme, String host, int port) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme(scheme);
        request.setServerName(host);
        request.setServerPort(port);
        return request;
    }
}
