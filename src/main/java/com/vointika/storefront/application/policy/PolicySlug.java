package com.vointika.storefront.application.policy;

import java.util.Locale;

/**
 * The address a policy type lives at, and the way back.
 *
 * <p><b>One place, because two of them must agree.</b> The globals already
 * publish `tourOperator.policies[].url` and the named accessors beside it, so the
 * page has to answer at exactly the address those links point to. The forward
 * rule used to be inline in {@code StorefrontGlobalsResponse}; the route needs
 * the inverse, and a second copy of a transform is how a link and its target
 * drift apart.
 *
 * <p><b>It does not validate.</b> {@code storefront} cannot see
 * {@code PolicyType} — it is a {@code touroperator} enum, and a copy here would
 * be a second list nothing keeps equal. An unknown slug simply becomes a type
 * name no operator has, and the read answers empty, which is the 404 the route
 * wants anyway.
 *
 * <p>{@code Locale.ROOT} on both sides, not the JVM default: under a Turkish
 * default {@code "LEGAL_NOTICE".toLowerCase()} is {@code "legaı_notice"}, and the
 * page would 404 on the machine that served it (`PATTERNS.md` §11).
 */
public final class PolicySlug {

    private PolicySlug() {
    }

    /** {@code LEGAL_NOTICE} → {@code legal-notice}. */
    public static String of(String typeName) {
        return typeName.toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /** {@code legal-notice} → {@code LEGAL_NOTICE}. Any slug maps; only real types resolve. */
    public static String toTypeName(String slug) {
        return slug.toUpperCase(Locale.ROOT).replace('-', '_');
    }
}
