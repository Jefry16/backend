package com.vointika.rendering.application.usecase;

import com.vointika.rendering.application.dto.output.ExperienceRenderContext;
import com.vointika.rendering.application.dto.output.ShopRenderContext;
import com.vointika.rendering.application.service.TenantResolver;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.StorefrontExperienceQuery;

/**
 * One experience's page, addressed by the handle in its URL.
 *
 * <p>An unpublished, unknown or wrong-tenant handle is the same 404 — the
 * storefront must not distinguish "no such experience" from "not published yet",
 * or a draft becomes discoverable by probing slugs.
 */
public class GetExperienceRenderContextUseCase {

    private final TenantResolver tenantResolver;
    private final StorefrontExperienceQuery storefrontExperienceQuery;

    public GetExperienceRenderContextUseCase(TenantResolver tenantResolver,
                                             StorefrontExperienceQuery storefrontExperienceQuery) {
        this.tenantResolver = tenantResolver;
        this.storefrontExperienceQuery = storefrontExperienceQuery;
    }

    public ExperienceRenderContext execute(String slug, String experienceSlug, String requestedLocale) {
        ShopRenderContext tenant = tenantResolver.resolve(slug, requestedLocale);

        return storefrontExperienceQuery
                .findPublishedBySlug(tenant.shop().id(), experienceSlug, tenant.locale())
                .map(experience -> new ExperienceRenderContext(
                        tenant.shop(), tenant.locale(), experience, tenant.navigation()))
                .orElseThrow(() -> new ResourceNotFoundException("Experience not found"));
    }
}
