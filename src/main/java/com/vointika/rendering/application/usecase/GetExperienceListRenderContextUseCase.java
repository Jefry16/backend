package com.vointika.rendering.application.usecase;

import com.vointika.rendering.application.dto.output.ExperienceListRenderContext;
import com.vointika.rendering.application.dto.output.ShopRenderContext;
import com.vointika.rendering.application.service.TenantResolver;
import com.vointika.shared.port.StorefrontExperienceQuery;

/**
 * Everything the experience-list page renders, in one response: the tenant, the
 * locale it resolved to, and its published experiences already translated.
 */
public class GetExperienceListRenderContextUseCase {

    private final TenantResolver tenantResolver;
    private final StorefrontExperienceQuery storefrontExperienceQuery;

    public GetExperienceListRenderContextUseCase(TenantResolver tenantResolver,
                                                 StorefrontExperienceQuery storefrontExperienceQuery) {
        this.tenantResolver = tenantResolver;
        this.storefrontExperienceQuery = storefrontExperienceQuery;
    }

    /** @param cursor opaque page cursor, or null for the first page */
    public ExperienceListRenderContext execute(String slug, String requestedLocale, String cursor) {
        ShopRenderContext tenant = tenantResolver.resolve(slug, requestedLocale);

        // An operator with nothing published gets an empty list, not a 404 —
        // the page exists, it just has nothing on it yet.
        return new ExperienceListRenderContext(
                tenant.shop(),
                tenant.locale(),
                storefrontExperienceQuery.listPublished(tenant.shop().id(), tenant.locale(), cursor));
    }
}
