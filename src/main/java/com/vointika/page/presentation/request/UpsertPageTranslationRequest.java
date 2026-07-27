package com.vointika.page.presentation.request;

/**
 * One locale's overlay: any field blank/absent = untranslated (falls back to
 * canonical). {@code slug} = the optional localized handle; absent with a
 * translated title derives one, absent without a title means the canonical
 * handle serves the locale.
 */
public record UpsertPageTranslationRequest(
        String title,
        String body,
        String seoTitle,
        String seoDescription,
        String slug) {
}
