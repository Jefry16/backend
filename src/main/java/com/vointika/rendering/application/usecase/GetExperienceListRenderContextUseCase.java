package com.vointika.rendering.application.usecase;

import com.vointika.rendering.application.dto.output.ExperienceListRenderContext;
import com.vointika.rendering.application.service.LocaleResolver;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.StorefrontExperienceQuery;
import com.vointika.shared.port.StorefrontOperatorQuery;
import com.vointika.shared.port.StorefrontOperatorView;

/**
 * Everything the experience-list page renders, in one response: the tenant, the
 * locale it resolved to, and its published experiences already translated.
 */
public class GetExperienceListRenderContextUseCase {

    private final StorefrontOperatorQuery storefrontOperatorQuery;
    private final StorefrontExperienceQuery storefrontExperienceQuery;

    public GetExperienceListRenderContextUseCase(StorefrontOperatorQuery storefrontOperatorQuery,
                                                 StorefrontExperienceQuery storefrontExperienceQuery) {
        this.storefrontOperatorQuery = storefrontOperatorQuery;
        this.storefrontExperienceQuery = storefrontExperienceQuery;
    }

    public ExperienceListRenderContext execute(String slug, String requestedLocale) {
        StorefrontOperatorView operator = storefrontOperatorQuery.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Storefront not found"));

        String locale = LocaleResolver.resolve(operator, requestedLocale);

        // An operator with nothing published gets an empty list, not a 404 —
        // the page exists, it just has nothing on it yet.
        return new ExperienceListRenderContext(
                operator,
                locale,
                storefrontExperienceQuery.listPublished(operator.id(), locale));
    }
}
