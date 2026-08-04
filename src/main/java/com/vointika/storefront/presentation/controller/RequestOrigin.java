package com.vointika.storefront.presentation.controller;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The absolute origin a storefront request arrived on — {@code https://acme.example.com},
 * with the port kept only when it is not the scheme's default.
 *
 * <p>It is derived from the request rather than from configuration for the reason
 * {@code StorefrontHomeController} gives for {@code getServerName()}: behind a
 * proxy, {@code ForwardedHeaderFilter} makes the servlet API honour
 * {@code X-Forwarded-Proto} and {@code X-Forwarded-Host}, so the origin follows
 * the address the visitor actually used. Building it from
 * {@code app.storefront.base-domain} instead would hard-code one scheme and lose
 * the dev port.
 *
 * <p>The host is canonical by construction: a request only resolves to a tenant
 * when it arrives on {@code {handle}.{base-domain}}, so there is no second address
 * a page could be reached at and no wrong host to bake into a canonical link.
 */
final class RequestOrigin {

    private RequestOrigin() {
    }

    static String of(HttpServletRequest request) {
        String scheme = request.getScheme();
        int port = request.getServerPort();
        boolean defaultPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
        return scheme + "://" + request.getServerName() + (defaultPort ? "" : ":" + port);
    }
}
