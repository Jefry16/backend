package com.vointika.rendering.application.usecase;

import com.vointika.rendering.application.dto.output.ShopRenderContext;
import com.vointika.rendering.application.service.TenantResolver;

/**
 * Resolves a storefront request's tenant and locale — the first thing every
 * public page render does, and all the password gate ever needs.
 *
 * <p>Unauthenticated in the JWT sense: {@code /api/internal/**} is gated by the
 * shared secret instead, so the only caller is the storefront BFF.
 */
public class GetShopRenderContextUseCase {

    private final TenantResolver tenantResolver;

    public GetShopRenderContextUseCase(TenantResolver tenantResolver) {
        this.tenantResolver = tenantResolver;
    }

    public ShopRenderContext execute(String slug, String requestedLocale) {
        return tenantResolver.resolve(slug, requestedLocale);
    }
}
