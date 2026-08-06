package com.vointika.touroperator.domain.enums;

/**
 * The social networks a brand can link to — a closed list rather than an
 * arbitrary URL bag, because a theme renders one icon per platform and cannot
 * render an icon for a name it has never heard of.
 *
 * <p><b>Not Shopify's list.</b> V10 took their nine verbatim and V11 replaced
 * them: theirs is an e-commerce list carrying Tumblr, Snapchat and Vimeo, and
 * missing the two a tour operator runs on. TripAdvisor is where a tour is
 * reviewed, and reviews are most of what converts a booking; WhatsApp is the
 * primary contact channel for operators across much of Latin America, southern
 * Europe and southeast Asia. Being closed is what makes the list have to be the
 * right one.
 *
 * <p>It pairs with the {@code tour_operator_brand_social_links} CHECK
 * constraint, and the two must agree: adding a platform here without widening
 * the constraint makes every insert of it fail with SQLSTATE 23514, which
 * nothing translates. That drift has shipped once already
 * ({@code audit_log_actor_type}), so {@code BrandEnumsMatchTheCheckConstraintsTest}
 * reads the migration and fails the build instead.
 */
public enum BrandSocialPlatform {
    FACEBOOK,
    INSTAGRAM,
    TIKTOK,
    YOUTUBE,
    TWITTER,
    PINTEREST,
    TRIPADVISOR,
    WHATSAPP
}
