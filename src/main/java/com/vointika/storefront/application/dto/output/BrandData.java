package com.vointika.storefront.application.dto.output;

import java.util.List;

/**
 * The operator's brand in <b>key</b> form — Shopify's {@code shop.brand}, and
 * the only place a logo exists: their shop object has none and neither does
 * ours.
 *
 * <p><b>Never null.</b> An operator with no brand row gets this record with null
 * fields and empty lists, so {@code shop.brand.slogan} means the same thing on
 * every page whether the row exists or not.
 *
 * @param colors      already split by role and ordered within each — the port
 *                    answers one ordered list and the split happens here, so
 *                    presentation never has to know what a role is
 * @param socialLinks in a stable order, because a set has none of its own
 */
public record BrandData(
        String slogan,
        String shortDescription,
        ColorsData colors,
        ImageData logo,
        ImageData squareLogo,
        ImageData favicon,
        ImageData coverImage,
        List<SocialLinkData> socialLinks
) {

    /**
     * The two roles Shopify's palette has, each an ordered list rather than a
     * value. Both are always present and may be empty — a theme indexing
     * {@code colors.primary} should find a list, not a missing key.
     */
    public record ColorsData(List<ColorData> primary, List<ColorData> secondary) {}

    /** One palette entry: what to paint behind, and what reads on top of it. */
    public record ColorData(String background, String foreground) {}

    /** @param platform a {@code BrandSocialPlatform} name — the enum stays in the context that owns it. */
    public record SocialLinkData(String platform, String url) {}
}
