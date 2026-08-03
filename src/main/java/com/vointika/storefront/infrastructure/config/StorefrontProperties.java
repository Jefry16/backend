package com.vointika.storefront.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param baseDomain the domain every storefront is a subdomain of. A request's
 *                   tenant is the one label in front of it, so this decides what
 *                   {@code TenantHandleResolver} will accept — {@code localhost}
 *                   in dev ({@code {handle}.localhost} resolves to 127.0.0.1 on
 *                   Linux with no DNS and no hosts file), the real storefront
 *                   domain in production. Configuration, never a constant.
 */
@ConfigurationProperties(prefix = "app.storefront")
public record StorefrontProperties(String baseDomain) {}
