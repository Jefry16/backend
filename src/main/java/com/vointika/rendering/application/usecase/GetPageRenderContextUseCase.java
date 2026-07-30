package com.vointika.rendering.application.usecase;

import com.vointika.rendering.application.dto.output.PageRenderContext;
import com.vointika.rendering.application.dto.output.ShopRenderContext;
import com.vointika.rendering.application.service.TenantResolver;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.StorefrontPageQuery;

/**
 * A CMS page, addressed by the handle in its URL.
 *
 * <p>Unknown, unpublished and wrong-tenant are the same 404, for the same reason
 * they are on an experience: a distinct response would let anyone confirm a
 * draft exists by guessing handles.
 */
public class GetPageRenderContextUseCase {

    private final TenantResolver tenantResolver;
    private final StorefrontPageQuery storefrontPageQuery;

    public GetPageRenderContextUseCase(TenantResolver tenantResolver,
                                       StorefrontPageQuery storefrontPageQuery) {
        this.tenantResolver = tenantResolver;
        this.storefrontPageQuery = storefrontPageQuery;
    }

    public PageRenderContext execute(String slug, String pageHandle, String requestedLocale) {
        ShopRenderContext tenant = tenantResolver.resolve(slug, requestedLocale);

        return storefrontPageQuery
                .findPublishedByHandle(tenant.shop().id(), pageHandle, tenant.locale())
                .map(page -> new PageRenderContext(
                        tenant.shop(), tenant.locale(), page, tenant.navigation()))
                .orElseThrow(() -> new ResourceNotFoundException("Page not found"));
    }
}
