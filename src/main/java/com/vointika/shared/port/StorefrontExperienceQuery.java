package com.vointika.shared.port;

import com.vointika.shared.list.CursorPage;

import java.util.Collection;
import java.util.Map;
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

    /**
     * A page of the operator's published experiences, newest first.
     *
     * <p>Cursor-paginated through the shared list framework, like every other
     * tenant-scoped list in this codebase — a storefront catalogue grows, and an
     * unbounded response would grow with it. Filtering and sorting are
     * deliberately NOT exposed yet: the framework supports them, but nothing
     * renders them, and an unused query surface is one more thing to get wrong.
     *
     * @param cursor opaque page cursor from a previous call, or null for the
     *               first page
     */
    CursorPage<StorefrontExperienceView> listPublished(
            UUID tourOperatorId, String locale, String cursor);

    /**
     * One published experience by the handle in its URL.
     *
     * <p>Matches the localized slug for this locale <em>or</em> the canonical
     * slug, because an operator who translates a handle leaves the canonical one
     * addressable — old links, shared links and search results keep working.
     */
    Optional<StorefrontExperienceView> findPublishedBySlug(
            UUID tourOperatorId, String slug, String locale);

    /**
     * id → the handle each published experience has in this locale.
     *
     * <p>Batched, because navigation resolves every menu item at once and
     * renders on every page. An id that is unknown, unpublished or another
     * tenant's is simply absent — a menu item pointing at a draft drops out of
     * the menu rather than linking to a 404.
     */
    Map<UUID, String> publishedHandles(UUID tourOperatorId, Collection<UUID> ids, String locale);
}
