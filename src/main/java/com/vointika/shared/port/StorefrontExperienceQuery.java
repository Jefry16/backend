package com.vointika.shared.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Cross-context read of an operator's <em>published</em> experiences for the
 * public storefront. Implemented in {@code experience}.
 *
 * <p>Publication is filtered here rather than by the caller, so there is no way
 * to ask this seam for a draft: the storefront cannot leak unpublished work even
 * if a render context forgets to check.
 *
 * <p>Every method takes the locale and returns content already resolved for it —
 * translated fields overlaid on the canonical ones, with a per-field fallback.
 * Resolution belongs to the context that owns the translations, not to the
 * renderer.
 */
public interface StorefrontExperienceQuery {

    /** The operator's published experiences, newest first. */
    List<StorefrontExperienceView> listPublished(UUID tourOperatorId, String locale);

    /**
     * One published experience by the handle in its URL.
     *
     * <p>Matches the localized slug for this locale <em>or</em> the canonical
     * slug, because an operator who translates a handle leaves the canonical one
     * addressable — old links, shared links and search results keep working.
     */
    Optional<StorefrontExperienceView> findPublishedBySlug(
            UUID tourOperatorId, String slug, String locale);
}
