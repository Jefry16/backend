package com.vointika.shared.port;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Cross-context read of an operator's <em>published</em> experiences for the
 * public storefront. Implemented in {@code experience}.
 *
 * <p>Publication is filtered here rather than by the caller, so there is no way
 * to ask this seam for a draft: the storefront cannot leak unpublished work even
 * if a render context forgets to check.
 *
 * <p>Reduced to the one method navigation needs. {@code listPublished} and
 * {@code findPublishedBySlug} went with the experience render contexts — the
 * detail read is what made a localized handle and a canonical one resolve as one
 * namespace, so PATTERNS §4d's read half is parked with it. The write guards it
 * justified are deliberately kept: they are what stops a shadowing handle being
 * stored while nothing can observe it.
 */
public interface StorefrontExperienceQuery {

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
