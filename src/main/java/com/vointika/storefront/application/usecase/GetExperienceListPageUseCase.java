package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.StorefrontExperienceQuery;
import com.vointika.shared.port.StorefrontExperienceQuery.StorefrontExperienceCard;
import com.vointika.shared.port.StorefrontShopQuery;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontGateView;
import com.vointika.storefront.application.dto.output.ExperienceListPageOutput;
import com.vointika.storefront.application.dto.output.ExperienceListPageOutput.ExperienceCard;
import com.vointika.storefront.application.dto.output.StorefrontPageData;
import com.vointika.storefront.application.policy.LocaleResolver;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The listing of what an operator sells, in the locale the path asks for, or
 * nothing. It mirrors {@link GetHomePageUseCase} exactly — gate row →
 * {@link LocaleResolver} → content — because the preamble belongs to the
 * request, not to the page: which tenant, which locale, is there a page here at
 * all.
 *
 * <p>Empty means "no page here": an unknown storefront, or a locale this
 * operator does not publish. <b>An operator with nothing published is not
 * empty</b> — it is a page with no cards, and answering 404 would mean a
 * storefront that disappears the day its last experience is unpublished.
 *
 * <p>It throws nothing: a miss is ordinary traffic on a public site.
 */
public class GetExperienceListPageUseCase {

    private final StorefrontShopQuery storefrontShopQuery;
    private final StorefrontExperienceQuery storefrontExperienceQuery;

    public GetExperienceListPageUseCase(StorefrontShopQuery storefrontShopQuery,
                                        StorefrontExperienceQuery storefrontExperienceQuery) {
        this.storefrontShopQuery = storefrontShopQuery;
        this.storefrontExperienceQuery = storefrontExperienceQuery;
    }

    /**
     * @param pathLocale the locale prefix in the URL, or {@code null} for the
     *                   unprefixed {@code /experiences}
     */
    public Optional<ExperienceListPageOutput> execute(String handle, String pathLocale) {
        return storefrontShopQuery.findGate(handle).flatMap(gate -> render(gate, pathLocale));
    }

    private Optional<ExperienceListPageOutput> render(StorefrontGateView gate, String pathLocale) {
        return LocaleResolver.resolve(pathLocale, gate.primaryLocale(), gate.supportedLocales())
                .flatMap(locale -> storefrontShopQuery.findContent(gate.tourOperatorId(), locale)
                        .map(shop -> new ExperienceListPageOutput(
                                StorefrontPageData.from(gate, shop, locale),
                                cards(gate.tourOperatorId(), locale))));
    }

    private List<ExperienceCard> cards(UUID tourOperatorId, String locale) {
        return storefrontExperienceQuery.findPublished(tourOperatorId, locale).stream()
                .map(GetExperienceListPageUseCase::toCard)
                .toList();
    }

    private static ExperienceCard toCard(StorefrontExperienceCard card) {
        return new ExperienceCard(
                card.handle(),
                card.name(),
                card.description(),
                card.thumbnailKey(),
                card.durationMinutes(),
                card.startingPrice());
    }
}
