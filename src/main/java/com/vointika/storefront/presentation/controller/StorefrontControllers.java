package com.vointika.storefront.presentation.controller;

import com.vointika.shared.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * The two things every storefront controller does the same way.
 *
 * <p><b>The 404 message is shared deliberately.</b> An unknown shop, a locale it
 * does not publish, a handle nothing answers to and a draft all answer
 * identically, and that only holds while every route says the same words —
 * copied into each controller, it is one edit away from telling a visitor which
 * kind of miss they hit.
 */
final class StorefrontControllers {

    private static final String NOT_FOUND = "There is no storefront at this address";

    private StorefrontControllers() {
    }

    /**
     * What {@code shop.url} is, and the base every absolute link is built on.
     * Read from the request rather than configuration so it stays right behind a
     * proxy — {@code ForwardedHeaderFilter} makes the servlet API honour
     * {@code X-Forwarded-Proto} and {@code X-Forwarded-Host}, the same reason the
     * tenant comes from {@code getServerName()}.
     */
    static String origin(HttpServletRequest request) {
        String scheme = request.getScheme();
        int port = request.getServerPort();
        boolean defaultPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
        return scheme + "://" + request.getServerName() + (defaultPort ? "" : ":" + port);
    }

    static ResourceNotFoundException notFound() {
        return new ResourceNotFoundException(NOT_FOUND);
    }
}
