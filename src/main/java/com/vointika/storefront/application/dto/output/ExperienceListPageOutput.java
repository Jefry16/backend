package com.vointika.storefront.application.dto.output;

import java.util.List;

/**
 * Everything the experiences listing renders: the shop bits the shared layout
 * needs, plus the cards, already in the order they appear. Media stay storage
 * keys — presentation resolves them to URLs (PATTERNS §5).
 *
 * <p>The title is the shop's, not the page's. There is no per-page-type SEO text
 * in the schema and a listing has no entity of its own to carry one; inventing a
 * field for it here would be a data model decided by a template (LAW §2.4).
 *
 * @param locale the locale that was actually rendered, which the page needs for
 *               its {@code <html lang>}
 */
public record ExperienceListPageOutput(
        String locale,
        String title,
        String shopName,
        String description,
        String logoKey,
        String ogImageKey,
        List<ExperienceCard> cards
) {

    /**
     * <p>No price. Every {@code starting_price} is {@code 0} and nothing
     * populates it, so a badge would render "from 0" on every card; the
     * propagator that fills the column is the next slice and adds the field with
     * a caller that means something.
     *
     * @param handle the experience's address <em>in this locale</em> — the
     *               localized handle when the translation carries one
     */
    public record ExperienceCard(
            String handle,
            String name,
            String description,
            String thumbnailKey,
            int durationMinutes
    ) {}
}
