package com.vointika.storefront.presentation.view;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.storefront.application.dto.output.ExperienceListPageOutput;
import com.vointika.storefront.application.dto.output.ExperienceListPageOutput.ExperienceCard;
import com.vointika.storefront.application.policy.StorefrontRoutes;

import java.util.List;

/**
 * The Mustache context object for the experiences listing — a view model, not a
 * serialized response.
 *
 * <p><b>Must stay public, and so must {@link CardView}.</b> The compiler runs
 * with access coercion off, so {@code Method.invoke} on a package-private class
 * throws {@code IllegalAccessException} at render time even when the accessor
 * itself is public — and a record nested in a public record is package-private
 * unless it says otherwise.
 *
 * <p>Two read-time resolutions happen here and nowhere upstream: a storage key
 * becomes a URL (PATTERNS §5), and a handle becomes a link.
 */
public record ExperienceListView(
        String locale,
        String title,
        String shopName,
        String description,
        String logoUrl,
        String ogImageUrl,
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
     *                   rendered locale</b>: the primary locale renders at
     *                   {@code /experiences} and its links must stay unprefixed,
     *                   or every card on the shop's own home locale would point
     *                   at a 404 by construction.
     */
    public static ExperienceListView from(ExperienceListPageOutput page, String pathLocale,
                                          MediaUrlResolver mediaUrlResolver) {
        String prefix = pathLocale == null ? "" : "/" + pathLocale;
        return new ExperienceListView(
                page.locale(),
                page.title(),
                page.shopName(),
                page.description(),
                mediaUrlResolver.toUrl(page.logoKey()),
                mediaUrlResolver.toUrl(page.ogImageKey()),
                page.cards().stream().map(card -> toCardView(card, prefix, mediaUrlResolver)).toList());
    }

    private static CardView toCardView(ExperienceCard card, String prefix, MediaUrlResolver mediaUrlResolver) {
        return new CardView(
                prefix + StorefrontRoutes.EXPERIENCES + "/" + card.handle(),
                card.name(),
                card.description(),
                mediaUrlResolver.toUrl(card.thumbnailKey()),
                card.durationMinutes());
    }
}
