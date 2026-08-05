package com.vointika.touroperator.domain.enums;

/**
 * The two colour roles Shopify's brand palette has, each an ordered list rather
 * than a single value — {@code colors.primary[0].background}.
 *
 * <p>It pairs with the {@code tour_operator_brand_colors} CHECK constraint, and
 * the two must agree: adding a role here without widening the constraint makes
 * every insert of it fail with SQLSTATE 23514, which nothing translates.
 * {@code BrandEnumsMatchTheCheckConstraintsTest} fails the build instead.
 */
public enum BrandColorRole {
    PRIMARY,
    SECONDARY
}
