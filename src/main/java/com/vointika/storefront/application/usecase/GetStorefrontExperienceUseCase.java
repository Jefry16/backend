package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.StorefrontExperienceQuery;
import com.vointika.shared.port.StorefrontExperienceQuery.ExperienceDetailView;
import com.vointika.shared.port.StorefrontMetafieldQuery;
import com.vointika.storefront.application.dto.output.StorefrontExperienceOutput;
import com.vointika.storefront.application.dto.output.StorefrontGlobals;
import com.vointika.storefront.application.policy.SeoText;

import java.util.Optional;

/**
 * One experience's page: the globals, the experience, and its custom data.
 *
 * <p><b>Globals first, and the order is load-bearing</b> — they carry the locale
 * decision, so an address in a locale the operator does not publish is a 404
 * before any experience is looked up. A draft experience and an unpublished
 * locale therefore cannot be told apart by trying both, which is the same
 * property the CMS page relies on.
 *
 * <p><b>The metafield read is given the id this use case resolved</b>, never one
 * from the URL. Values are owner-generic, so the operator is what scopes them;
 * pairing a resolved experience with its own operator is what keeps the answer
 * inside the tenant.
 *
 * <p><b>SEO is the experience's own, and no schema was invented for it.</b>
 * `OPEN-WORK.md` warns that this page would be the first to force the
 * per-page-type SEO decision — it is not, because {@code experiences} and their
 * overlay have carried {@code seo_title}/{@code seo_description} since
 * experience/V8. The chain is the one a CMS page already uses; what remains open
 * is page types with no entity of their own, which is the listing.
 */
public class GetStorefrontExperienceUseCase {

    private final GetStorefrontGlobalsUseCase getStorefrontGlobals;
    private final StorefrontExperienceQuery experienceQuery;
    private final StorefrontMetafieldQuery metafieldQuery;

    public GetStorefrontExperienceUseCase(GetStorefrontGlobalsUseCase getStorefrontGlobals,
                                          StorefrontExperienceQuery experienceQuery,
                                          StorefrontMetafieldQuery metafieldQuery) {
        this.getStorefrontGlobals = getStorefrontGlobals;
        this.experienceQuery = experienceQuery;
        this.metafieldQuery = metafieldQuery;
    }

    public Optional<StorefrontExperienceOutput> execute(String operatorHandle, String pathLocale, String handle) {
        return getStorefrontGlobals.execute(operatorHandle, pathLocale).flatMap(globals -> {
            String locale = globals.localization().current();
            return experienceQuery.findByHandle(globals.tourOperator().id(), handle, locale)
                    .map(experience -> new StorefrontExperienceOutput(
                            withExperienceSeo(globals, experience),
                            experience,
                            metafieldQuery.findForExperience(
                                    globals.tourOperator().id(), experience.id(), locale)));
        });
    }

    private static StorefrontGlobals withExperienceSeo(StorefrontGlobals globals, ExperienceDetailView experience) {
        return globals.withSeo(
                SeoText.title(experience.seoTitle(), experience.name(),
                        globals.tourOperator().seoTitle(), globals.tourOperator().name()),
                SeoText.description(experience.seoDescription(), globals.tourOperator().seoDescription()));
    }
}
