package com.vointika.shared.port;

import java.util.List;
import java.util.UUID;

/**
 * The storefront's read seam onto what an operator sells: the published
 * experiences of one tenant, in one locale, in the order the listing shows them.
 * Implemented by {@code experience}, which owns the rows.
 *
 * <p>The second seam the {@code storefront} context reaches through, after
 * {@link StorefrontShopQuery}, and the reason that context is fenced from
 * {@code experience} at all — a storefront may never import the admin surface's
 * types.
 *
 * <p><b>Ordering is part of the contract, not the caller's business.</b> A
 * listing that reorders between requests is a bug report, so the order is
 * decided once, here: <b>featured first, then oldest first, tie-broken on id</b>.
 *
 * <p>Every text field is <b>nullable-wins-canonical</b>: a translation row has
 * every content column nullable, so a null column falls back to the experience's
 * own value. A row overlays; it does not replace.
 */
public interface StorefrontExperienceQuery {

    /**
     * Every published experience of this operator, overlaid with the locale's
     * translation where there is one. An operator with nothing published gets an
     * empty list — that is a storefront with no cards, never an error.
     */
    List<StorefrontExperienceCard> findPublished(UUID tourOperatorId, String locale);

    /**
     * @param handle       the experience's address in this locale — the
     *                     translation's own handle when it carries one, the
     *                     canonical handle otherwise. Both namespaces are one
     *                     (PATTERNS §4d), which is why either can serve.
     * @param thumbnailKey a storage key, <b>never a URL</b> (PATTERNS §5), and
     *                     {@code null} when the experience has no thumbnail or
     *                     its media reference no longer resolves.
     *                     {@code null} rather than {@code ""} throughout: Mustache
     *                     treats the empty string as truthy by default, so a
     *                     section guarding an optional tag would emit it empty.
     */
    record StorefrontExperienceCard(
            String handle,
            String name,
            String description,
            String thumbnailKey,
            int durationMinutes,
            boolean featured
    ) {}
}
