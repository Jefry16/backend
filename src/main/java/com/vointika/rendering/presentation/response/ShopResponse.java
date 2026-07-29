package com.vointika.rendering.presentation.response;

import com.vointika.shared.port.StorefrontOperatorView;

import java.util.List;

/**
 * The {@code shop} block on the wire.
 *
 * <p>Deliberately without the {@code id}/{@code context} pair the admin API
 * responses carry (PATTERNS §4a): this is not a REST resource representation
 * for the SPA but a render block for a theme, addressed by slug, and an
 * operator id would be a field no consumer reads.
 */
public record ShopResponse(
        String name,
        String slug,
        String logoUrl,
        String primaryLocale,
        List<String> supportedLocales,
        String currency,
        String timezone,
        boolean passwordEnabled,
        String passwordMessage) {

    public static ShopResponse from(StorefrontOperatorView operator) {
        return new ShopResponse(
                operator.name(),
                operator.slug(),
                operator.logoUrl(),
                operator.primaryLocale(),
                operator.supportedLocales(),
                operator.currency(),
                operator.timezone(),
                operator.passwordEnabled(),
                operator.passwordMessage());
    }
}
