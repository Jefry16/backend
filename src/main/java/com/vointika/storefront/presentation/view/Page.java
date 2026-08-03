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
public record Page(String title, String description, String ogImageUrl) {

    public static Page from(PageData page, MediaUrlResolver mediaUrlResolver) {
        return new Page(page.title(), page.description(), mediaUrlResolver.toUrl(page.ogImageKey()));
    }
}
