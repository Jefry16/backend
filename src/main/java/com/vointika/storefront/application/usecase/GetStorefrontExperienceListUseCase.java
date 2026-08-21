package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.StorefrontExperienceQuery;
import com.vointika.storefront.application.dto.output.StorefrontExperienceListOutput;

import java.util.Optional;

/**
 * The experiences listing: the globals, plus a page of the operator's published
 * experiences in the rendered locale.
 *
 * <p>Empty is a legitimate answer and not a 404 — an operator with nothing
 * published has a listing page with nothing on it, the same way a shop with no
 * products still has a catalogue page. The only 404s here are the ones the
 * globals already answer: an unknown host and a locale the operator does not
 * publish.
 *
 * <p><b>The SEO text is the operator's, and that is a known gap rather than a
 * decision.</b> A CMS page overrides it from its own row; this listing has no
 * entity to carry one, so it repeats the shop's title. `OPEN-WORK.md` carries the
 * question of where per-page-type SEO text lives, and this is the page it was
 * filed about — deliberately not answered here, because a schema decided by a
 * template is the wrong order.
 */
public class GetStorefrontExperienceListUseCase {

    private final GetStorefrontGlobalsUseCase getStorefrontGlobals;
    private final StorefrontExperienceQuery experienceQuery;

    public GetStorefrontExperienceListUseCase(GetStorefrontGlobalsUseCase getStorefrontGlobals,
                                              StorefrontExperienceQuery experienceQuery) {
        this.getStorefrontGlobals = getStorefrontGlobals;
        this.experienceQuery = experienceQuery;
    }

    /**
     * @param cursor the opaque cursor from the previous page, or null for the
     *               first. It is the only thing a visitor contributes to the
     *               query — the tenant comes from the host and the rest is the
     *               adapter's.
     */
    public Optional<StorefrontExperienceListOutput> execute(String operatorHandle, String pathLocale,
                                                            String cursor) {
        return getStorefrontGlobals.execute(operatorHandle, pathLocale).map(globals ->
                new StorefrontExperienceListOutput(
                        globals,
                        experienceQuery.listPublished(
                                globals.tourOperator().id(),
                                globals.localization().current(),
                                cursor)));
    }
}
