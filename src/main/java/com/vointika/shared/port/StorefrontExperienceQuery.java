package com.vointika.shared.port;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The experiences the storefront's globals carry. Implemented in
 * {@code experience}.
 *
 * <p><b>Featured and published only, and capped.</b> Shopify's globals hand a
 * theme {@code collections} and {@code all_products} — accessors that cost
 * nothing until a template resolves one. Ours is a JSON payload, computed on
 * every page whether it is rendered or not, so "all products" is not a shape we
 * can copy. Featured is the merchant's own answer to "which few matter", and the
 * cap is there because {@code featured} is a checkbox nobody thinks of as a
 * performance decision.
 *
 * <p><b>The cap and the order are this port's contract, not the caller's.</b>
 * Both live in the derived query's name and nowhere else, so a mechanical rename
 * moves them together. A listing that reorders between requests is a bug report,
 * and the seeded operator's two featured experiences share a {@code created_at}
 * to the microsecond — so the id tie-break is load-bearing on day one, not
 * decoration.
 */
public interface StorefrontExperienceQuery {

    /** How many the globals carry, at most. The rest need the listing page. */
    int FEATURED_LIMIT = 12;

    /**
     * @param locale the locale already chosen by the storefront's locale rule;
     *               translations overlay nullable-wins-canonical, <b>including
     *               the handle</b>, because a localized handle is the address
     *               that locale's card has to link to
     */
    List<ExperienceCardView> findFeatured(UUID tourOperatorId, String locale);

    /**
     * Where these experiences live, for callers holding ids — a menu item points
     * at an experience and has to become a link.
     *
     * <p><b>Published only, and an id that is missing from the result is the
     * answer</b>: an unpublished or deleted experience has no address, so a
     * caller drops the link rather than rendering one that 404s. Same reason the
     * handle is the translated one — a link in a locale points at that locale's
     * address.
     */
    Map<UUID, String> findPublishedHandles(UUID tourOperatorId, Set<UUID> experienceIds, String locale);

    /**
     * @param startingPrice the lowest price across the experience's audiences,
     *                      maintained by the write paths. Crosses as a
     *                      {@code BigDecimal} so the caller decides how to render
     *                      it — a double would already have lost the cents.
     * @param thumbnailMediaId a media <b>id</b>, resolved with every other image
     *                         in the response in one batch (PATTERNS §5)
     */
    record ExperienceCardView(UUID id,
                              String handle,
                              String name,
                              String description,
                              BigDecimal startingPrice,
                              UUID thumbnailMediaId) {}
}
