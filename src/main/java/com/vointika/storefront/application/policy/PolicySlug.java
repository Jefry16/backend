package com.vointika.storefront.application.policy;

import java.util.Locale;

/**
 * The one place the storefront's URL vocabulary meets the policy type's:
 * {@code LEGAL_NOTICE} is addressed at {@code /policies/legal-notice}.
 *
 * <p><b>A transform, not a second list.</b> Writing the four pairs out here
 * would be a third copy of the closed set — after the enum and the CHECK
 * constraint, which already have to agree — and a copy nothing would fail to
 * keep true. Lower-casing and swapping the separator is total in both
 * directions, so a type added to the enum is addressable the same day.
 *
 * <p>It follows that <b>an unknown slug is not rejected here</b>:
 * {@code /policies/refunds} becomes the name {@code REFUNDS} and the query port
 * answers empty for it, which is the 404 the page needs. The one thing that must
 * not happen is {@code PolicyType.valueOf} throwing on the way, and that is why
 * the port takes a name and looks it up rather than converting it.
 *
 * <p>{@link StorefrontRoutes#POLICY} constrains the segment to lowercase, so the
 * round trip is one-to-one: {@code /policies/Terms} never reaches this class.
 *
 * <p>Case-folded with {@link Locale#ROOT}, never the JVM default — under a
 * Turkish default {@code "LEGAL_NOTICE".toLowerCase()} yields a dotless
 * {@code ı} and the link stops matching the route depending on which machine
 * served the request.
 */
public final class PolicySlug {

    private PolicySlug() {
    }

    /** {@code LEGAL_NOTICE} → {@code legal-notice}. */
    public static String of(String type) {
        return type.toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /** {@code legal-notice} → {@code LEGAL_NOTICE}, whether or not a type is named that. */
    public static String toType(String slug) {
        return slug.toUpperCase(Locale.ROOT).replace('-', '_');
    }
}
