package com.vointika.page.presentation.request;

/** Whole replace of the editable content; null/blank SEO fields clear them. */
public record UpdatePageRequest(
        String title,
        String body,
        String seoTitle,
        String seoDescription) {
}
