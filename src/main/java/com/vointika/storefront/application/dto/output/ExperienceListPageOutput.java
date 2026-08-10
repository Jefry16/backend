package com.vointika.storefront.application.dto.output;

import java.math.BigDecimal;
import java.util.List;

/**
 * The envelope every page gets, plus the one thing this page has of its own:
 * the cards, already in the order they appear.
 *
 * <p>The page title is the shop's. There is no per-page-type SEO text in the
 * schema and a listing has no entity of its own to carry one; inventing a field
 * for it here would be a data model decided by a template (LAW §2.4).
 */
public record ExperienceListPageOutput(StorefrontPageData envelope, List<ExperienceCard> cards) {

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
            int durationMinutes,
            BigDecimal startingPrice
    ) {}
}
