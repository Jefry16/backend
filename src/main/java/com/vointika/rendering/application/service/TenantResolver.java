package com.vointika.rendering.application.service;

import com.vointika.rendering.application.dto.output.ShopRenderContext;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.StorefrontOperatorQuery;
import com.vointika.shared.port.StorefrontOperatorView;

/**
 * The first two steps of every storefront render: which tenant, and in which
 * locale.
 *
 * <p>Both answers are needed before any page can fetch its content — the tenant
 * id keys every other cross-context query, and the resolved locale decides which
 * translations that content comes back in. Keeping them in one place means a new
 * page type cannot accidentally fetch its content in a locale the chrome is not
 * rendering, which is the failure this would otherwise invite: two lines copied
 * into a new use case, one of them subtly wrong.
 *
 * <p>The result is a {@link ShopRenderContext} because that is exactly what a
 * resolved tenant is — the chrome, with no page content attached.
 */
public class TenantResolver {

    private final StorefrontOperatorQuery storefrontOperatorQuery;

    public TenantResolver(StorefrontOperatorQuery storefrontOperatorQuery) {
        this.storefrontOperatorQuery = storefrontOperatorQuery;
    }

    /**
     * @param requestedLocale the locale the URL asked for, or null for the bare
     *                        (prefix-less) path
     * @throws ResourceNotFoundException when no operator holds this slug, so an
     *                                   unknown subdomain renders the platform's
     *                                   404 rather than a broken tenant page
     */
    public ShopRenderContext resolve(String slug, String requestedLocale) {
        StorefrontOperatorView operator = storefrontOperatorQuery.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Storefront not found"));

        return new ShopRenderContext(operator, LocaleResolver.resolve(operator, requestedLocale));
    }
}
