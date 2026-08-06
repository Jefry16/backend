package com.vointika.touroperator.domain.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * The operator's legal documents. <b>The type is the address</b> — a policy has
 * no handle, because there is exactly one of each — so this closed list is also
 * the storefront's {@code /policies/…} namespace.
 *
 * <p><b>Four, not Shopify's six.</b> Their <em>Shipping</em> and
 * <em>Subscription</em> have no analogue here (nothing ships), and their
 * <em>Return</em> is our {@link #CANCELLATION}, which is the single most-asked
 * question about a tour. A closed list has to be the right closed list — the same
 * argument {@link BrandSocialPlatform} was narrowed under.
 *
 * <p>It pairs with the {@code tour_operator_policies} CHECK constraint and the
 * two must agree both ways: a type the constraint lacks fails every insert with
 * SQLSTATE 23514, which nothing translates, and a value the enum lacks fails the
 * <em>read</em> on a public page. {@code PolicyTypeMatchesTheCheckConstraintTest}
 * reads the migration and fails the build instead.
 */
public enum PolicyType {
    CANCELLATION,
    PRIVACY,
    TERMS,
    LEGAL_NOTICE;

    /**
     * The type a name addresses, or empty when no type does.
     *
     * <p>It exists so that a storefront URL nobody wrote a policy for is a
     * <b>404</b>: the name arrives from a path segment, and {@code valueOf} would
     * answer {@code /policies/refunds} with an {@code IllegalArgumentException} —
     * a 500, in the admin API's JSON error shape, on a public HTML page.
     */
    public static Optional<PolicyType> from(String name) {
        return Arrays.stream(values()).filter(type -> type.name().equals(name)).findFirst();
    }
}
