package com.vointika.shared.port;

import java.util.Optional;

/**
 * The storefront's read seam onto the tenant: everything the public site needs
 * to render a shop's chrome, addressed by the handle carried in the request's
 * host. Implemented by {@code touroperator}, which owns the row.
 *
 * <p>This is the first port the {@code storefront} context reaches through, and
 * the reason that context is fenced from {@code touroperator} at all — a
 * storefront may never import the admin surface's types.
 */
public interface StorefrontShopQuery {

    Optional<StorefrontShopView> findByHandle(String handle);

    /**
     * Media are <b>storage keys, never URLs</b> (PATTERNS §5): the absolute URL
     * is built at read time against the current asset base. An unset — or
     * since-deleted — media reference is absent from the media library lookup
     * and arrives here as {@code null}.
     *
     * <p>Absent SEO fields are {@code null}, never {@code ""}: Mustache's default
     * truthiness treats the empty string as <em>truthy</em>, so a section
     * guarding an optional tag would emit it empty. (The compiler also sets
     * {@code emptyStringIsFalse}; both, belt and braces.)
     */
    record StorefrontShopView(
            String name,
            String handle,
            String logoKey,
            String ogImageKey,
            String seoTitle,
            String seoDescription
    ) {}
}
