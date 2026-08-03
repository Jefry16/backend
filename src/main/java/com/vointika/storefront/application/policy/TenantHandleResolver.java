package com.vointika.storefront.application.policy;

import java.util.Locale;
import java.util.Optional;

/**
 * Host → tenant handle. {@code acme.localhost:8080} is {@code acme}; anything
 * else is nobody.
 *
 * <p>Two rejections are load-bearing and carried over from the archived
 * storefront. The <b>apex</b> ({@code localhost}) addresses no tenant, and a
 * <b>multi-label</b> host ({@code a.b.localhost}) must not either — otherwise a
 * crafted host could be read as a tenant it is not.
 *
 * <p>It takes the host as a string and knows nothing about servlets, which is
 * why it can live here: it is the storefront's rule about who a request is for,
 * and both the controller that supplies the header and the config that supplies
 * the base domain may depend on this layer, while they may not depend on each
 * other.
 *
 * <p>It takes a host in either form a caller can hold one — with or without a
 * port — and strips the port itself. The controller feeds it
 * {@code getServerName()}, which never carries one, so that branch is
 * normalization rather than a live path; it stays because the argument is
 * a <em>host</em> and a partial function over host strings is the kind of
 * sharp edge that gets rediscovered by a caller, not by a test.
 */
public class TenantHandleResolver {

    private final String suffix;

    public TenantHandleResolver(String baseDomain) {
        this.suffix = "." + baseDomain.toLowerCase(Locale.ROOT);
    }

    public Optional<String> resolve(String host) {
        if (host == null || host.isBlank()) {
            return Optional.empty();
        }
        // PATTERNS §11: Locale.ROOT, never the JVM default — under a Turkish
        // default "IT".toLowerCase() is "ıt" and the handle stops matching.
        String name = stripPort(host.trim()).toLowerCase(Locale.ROOT);
        if (!name.endsWith(suffix)) {
            return Optional.empty();
        }
        String label = name.substring(0, name.length() - suffix.length());
        if (label.isEmpty() || label.indexOf('.') >= 0) {
            return Optional.empty();
        }
        return Optional.of(label);
    }

    private static String stripPort(String host) {
        int colon = host.indexOf(':');
        return colon >= 0 ? host.substring(0, colon) : host;
    }
}
