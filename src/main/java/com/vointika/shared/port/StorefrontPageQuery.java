package com.vointika.shared.port;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Where a CMS page lives, for callers holding ids. Implemented in {@code page}.
 *
 * <p>The twin of {@code StorefrontExperienceQuery.findPublishedHandles}, and it
 * exists separately for the reason every seam here does: the context that owns
 * the row is the one that answers for it.
 *
 * <p><b>Published only, and an id missing from the result is the answer</b> — an
 * unpublished or deleted page has no address, so a menu item pointing at one is
 * dropped rather than rendered as a link that 404s.
 */
public interface StorefrontPageQuery {

    Map<UUID, String> findPublishedHandles(UUID tourOperatorId, Set<UUID> pageIds, String locale);
}
