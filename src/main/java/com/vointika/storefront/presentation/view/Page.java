package com.vointika.storefront.presentation.view;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.storefront.application.dto.output.PageData;

/**
 * {@code {{page}}} — this page's own head values, separate from {@link Shop} so
 * that {@code page.title} still means something on a page that is not the shop's
 * front door.
 *
 * <p>Public for the reason {@link Shop} documents.
 */
public record Page(String title, String description, String ogImageUrl, String path) {

    /**
     * @param path this page's own path, which {@code routes} cannot give: routes
     *             says where each page <em>lives</em>, not which one you are on.
     *             Paired with {@code shop.url} it is the canonical address —
     *             Shopify keeps the same two facts apart, as {@code shop.url} and
     *             a separate global {@code canonical_url}.
     */
    public static Page from(PageData page, MediaUrlResolver mediaUrlResolver, String path) {
        return new Page(page.title(), page.description(),
                mediaUrlResolver.toUrl(page.ogImageKey()), path);
    }
}
