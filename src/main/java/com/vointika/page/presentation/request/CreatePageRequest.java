package com.vointika.page.presentation.request;

/** Create a page: operator-chosen handle, raw-HTML body, optional SEO overrides. */
public record CreatePageRequest(
        String title,
        String handle,
        String body,
        String seoTitle,
        String seoDescription) {
}
