package com.vointika.rendering.application.usecase;

import com.vointika.rendering.application.dto.output.ExperienceRenderContext;
import com.vointika.rendering.application.service.LocaleResolver;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.StorefrontExperienceQuery;
import com.vointika.shared.port.StorefrontOperatorQuery;
import com.vointika.shared.port.StorefrontOperatorView;

/**
 * One experience's page, addressed by the handle in its URL.
 *
 * <p>An unpublished, unknown or wrong-tenant handle is the same 404 — the
 * storefront must not distinguish "no such experience" from "not published yet",
 * or a draft becomes discoverable by probing slugs.
 */
public class GetExperienceRenderContextUseCase {

    private final StorefrontOperatorQuery storefrontOperatorQuery;
    private final StorefrontExperienceQuery storefrontExperienceQuery;

    public GetExperienceRenderContextUseCase(StorefrontOperatorQuery storefrontOperatorQuery,
                                             StorefrontExperienceQuery storefrontExperienceQuery) {
        this.storefrontOperatorQuery = storefrontOperatorQuery;
        this.storefrontExperienceQuery = storefrontExperienceQuery;
    }

    public ExperienceRenderContext execute(String slug, String experienceSlug, String requestedLocale) {
        StorefrontOperatorView operator = storefrontOperatorQuery.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Storefront not found"));

        String locale = LocaleResolver.resolve(operator, requestedLocale);

        return storefrontExperienceQuery
                .findPublishedBySlug(operator.id(), experienceSlug, locale)
                .map(experience -> new ExperienceRenderContext(operator, locale, experience))
                .orElseThrow(() -> new ResourceNotFoundException("Experience not found"));
    }
}
