package com.vointika.storefront.application.dto.output;

/**
 * Everything the home template renders, with the shop's fallbacks already
 * applied. Media stay storage keys — presentation resolves them to URLs
 * (PATTERNS §5).
 */
public record HomePageOutput(
        String title,
        String shopName,
        String description,
        String logoKey,
        String ogImageKey
) {}
