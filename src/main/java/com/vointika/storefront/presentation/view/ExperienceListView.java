package com.vointika.storefront.presentation.view;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.storefront.application.dto.output.ExperienceListPageOutput;
import com.vointika.storefront.application.dto.output.ExperienceListPageOutput.ExperienceCard;

import java.util.List;

/**
 * The Mustache context object for the experiences listing: the four global
 * objects every page gets, plus the collection this page exists to show.
 *
 * <p><b>Must stay public, and so must {@link CardView}</b> — see
 * {@link HomeView}, which also records why the four components are repeated
 * rather than nested.
 *
 * <p>Two read-time resolutions happen here and nowhere upstream: a storage key
 * becomes a URL (PATTERNS §5), and a handle becomes a link.
 */
public record ExperienceListView(
        Shop shop,
        Page page,
        Routes routes,
        Localization localization,
        List<CardView> experiences
) {

    /**
     * @param url the experience's page, <b>which does not exist yet</b> — the
     *            detail route is the next slice, so every card links to a 404
     *            today. The link is here because it is what gives the localized
     *            handle a reader, and the URL shape is already decided.
     */
    public record CardView(
            String url,
            String name,
            String description,
            String thumbnailUrl,
            int durationMinutes
    ) {}

    /**
     * @param pathLocale the locale prefix the request arrived under, or
     *                   {@code null} for the unprefixed listing. <b>Not the
     *                   rendered locale</b> — see {@link Routes}.
     */
    public static ExperienceListView from(ExperienceListPageOutput page, String pathLocale,
                                          MediaUrlResolver mediaUrlResolver) {
        Routes routes = Routes.forPathLocale(pathLocale);
        return new ExperienceListView(
                Shop.from(page.envelope().shop(), mediaUrlResolver),
                Page.from(page.envelope().page(), mediaUrlResolver),
                routes,
                Localization.from(page.envelope().localization(), Routes::experiences),
                page.cards().stream().map(card -> toCardView(card, routes, mediaUrlResolver)).toList());
    }

    private static CardView toCardView(ExperienceCard card, Routes routes, MediaUrlResolver mediaUrlResolver) {
        return new CardView(
                routes.experiences() + "/" + card.handle(),
                card.name(),
                card.description(),
                mediaUrlResolver.toUrl(card.thumbnailKey()),
                card.durationMinutes());
    }
}
