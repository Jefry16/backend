package com.vointika.shared.port;

import java.util.Optional;
import java.util.UUID;

/**
 * Cross-context read of an operator's <em>published</em> CMS pages for the public
 * storefront. Implemented in {@code page}.
 *
 * <p>Like the experience seam: publication is filtered here so a draft cannot
 * reach a public page through a forgotten check, and the translation overlay is
 * applied where the translations live.
 */
public interface StorefrontPageQuery {

    /**
     * One published page by the handle in its URL.
     *
     * <p>Matches the localized handle for this locale <em>or</em> the canonical
     * one — an operator who translates a handle leaves the original addressable,
     * so existing links keep working.
     */
    Optional<StorefrontPageView> findPublishedByHandle(
            UUID tourOperatorId, String handle, String locale);
}
