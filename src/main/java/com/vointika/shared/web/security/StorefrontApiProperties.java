package com.vointika.shared.web.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the storefront-API auth model.
 *
 * <p>{@code /api/storefront/**} routes are server-to-server (called by the
 * storefront SSR Node app, not a browser). They are not JWT-gated; instead,
 * callers must send {@code X-Storefront-Secret: <sharedSecret>}. The secret
 * lives outside the binary — set {@code APP_STOREFRONT_SHARED_SECRET} in the
 * environment.
 */
@ConfigurationProperties(prefix = "app.storefront")
public record StorefrontApiProperties(String sharedSecret) {}
