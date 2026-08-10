package com.vointika.storefront.presentation.view;

import java.math.RoundingMode;
import java.math.BigDecimal;
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
            int durationMinutes,
            String startingPrice
    ) {}

    /**
     * @param pathLocale the locale prefix the request arrived under, or
     *                   {@code null} for the unprefixed listing. <b>Not the
     *                   rendered locale</b> — see {@link Routes}.
     */
    public static ExperienceListView from(ExperienceListPageOutput page, String pathLocale,
                                          MediaUrlResolver mediaUrlResolver,
                                       String origin, String path) {
        Routes routes = Routes.forPathLocale(pathLocale);
        return new ExperienceListView(
                Shop.from(page.envelope().shop(), mediaUrlResolver, origin, routes),
                Page.from(page.envelope().page(), mediaUrlResolver, path),
                routes,
                Localization.from(page.envelope().localization(), Routes::experiences),
                page.cards().stream().map(card -> toCardView(card, routes, mediaUrlResolver)).toList());
    }

    /**
     * The bare amount. Always present — an experience cannot exist without a
     * price greater than zero — so there is no empty case for the template to
     * guard.
     *
     * <p>No currency symbol: {@code shop.currency} carries it, so a theme wanting
     * {@code 35,00 €} rather than {@code €35.00} can place it itself.
     */
    private static String price(BigDecimal startingPrice) {
        return startingPrice.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static CardView toCardView(ExperienceCard card, Routes routes, MediaUrlResolver mediaUrlResolver) {
        return new CardView(
                routes.experiences() + "/" + card.handle(),
                card.name(),
                card.description(),
                mediaUrlResolver.toUrl(card.thumbnailKey()),
                card.durationMinutes(),
                price(card.startingPrice()));
    }
}
