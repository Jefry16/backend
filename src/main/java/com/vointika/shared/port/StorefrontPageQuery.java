package com.vointika.shared.port;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Cross-context read of an operator's <em>published</em> CMS pages for the public
 * storefront. Implemented in {@code page}.
 *
 * <p>Like the experience seam: publication is filtered here so a draft cannot
 * reach a public page through a forgotten check, and the translation overlay is
 * applied where the translations live.
 *
 * <p>Reduced to the one method navigation needs — {@code findPublishedByHandle}
 * went with the page render context. See {@link StorefrontExperienceQuery} for
 * what that means for PATTERNS §4d.
 */
public interface StorefrontPageQuery {

    /**
     * id → the handle each published page has in this locale. Same contract as
     * the experience seam: absent means "do not link to it".
     */
    Map<UUID, String> publishedHandles(UUID tourOperatorId, Collection<UUID> ids, String locale);
}
