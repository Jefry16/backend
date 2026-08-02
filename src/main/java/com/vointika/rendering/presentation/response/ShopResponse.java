package com.vointika.rendering.presentation.response;

import com.vointika.shared.port.StorefrontOperatorView;

import java.util.List;

/**
 * The {@code shop} block on the wire.
 *
 * <p>Deliberately without the {@code id}/{@code context} pair the admin API
 * responses carry (PATTERNS §4a): this is not a REST resource representation
 * for the SPA but a render block for a theme, addressed by handle, and an
 * operator id would be a field no consumer reads.
 */
public record ShopResponse(
        String name,
        String handle,
        String logoUrl,
        String primaryLocale,
        List<String> supportedLocales,
        String currency,
        String timezone,
        boolean passwordEnabled,
        String passwordMessage) {

    /**
     * @param passwordMessage the locale-resolved message, not
     *                        {@code operator.passwordMessage()} — the canonical
     *                        value is the fallback, never what gets rendered.
     */
    public static ShopResponse from(StorefrontOperatorView operator, String passwordMessage) {
        return new ShopResponse(
                operator.name(),
                operator.handle(),
                operator.logoUrl(),
                operator.primaryLocale(),
                operator.supportedLocales(),
                operator.currency(),
                operator.timezone(),
                operator.passwordEnabled(),
                passwordMessage);
    }
}
