package com.vointika.rendering.application.usecase;

import com.vointika.rendering.application.dto.output.ShopRenderContext;
import com.vointika.rendering.application.service.LocaleResolver;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.StorefrontOperatorQuery;
import com.vointika.shared.port.StorefrontOperatorView;

/**
 * Resolves a storefront request's tenant and locale — the first thing every
 * public page render does, and all the password gate ever needs.
 *
 * <p>Unauthenticated in the JWT sense: {@code /api/internal/**} is gated by the
 * shared secret instead, so the only caller is the storefront BFF.
 */
public class GetShopRenderContextUseCase {

    private final StorefrontOperatorQuery storefrontOperatorQuery;

    public GetShopRenderContextUseCase(StorefrontOperatorQuery storefrontOperatorQuery) {
        this.storefrontOperatorQuery = storefrontOperatorQuery;
    }

    /**
     * @param requestedLocale the locale the URL asked for, or null for the bare
     *                        (prefix-less) path — see {@link LocaleResolver}
     * @throws ResourceNotFoundException when no operator holds this slug, so an
     *                                   unknown subdomain renders the platform's
     *                                   404 rather than a broken tenant page
     */
    public ShopRenderContext execute(String slug, String requestedLocale) {
        StorefrontOperatorView operator = storefrontOperatorQuery.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Storefront not found"));

        return new ShopRenderContext(operator, LocaleResolver.resolve(operator, requestedLocale));
    }
}
