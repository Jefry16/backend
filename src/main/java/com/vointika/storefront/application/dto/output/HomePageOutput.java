package com.vointika.storefront.application.dto.output;

/**
 * Everything the home template renders, with the shop's fallbacks already
 * applied. Media stay storage keys — presentation resolves them to URLs
 * (PATTERNS §5).
 *
 * @param locale the locale that was actually rendered, which the page needs for
 *               its {@code <html lang>}
 */
public record HomePageOutput(
        String locale,
        String title,
        String shopName,
        String description,
        String logoKey,
        String ogImageKey
) {}
