package com.vointika.page.presentation.response;

import com.vointika.page.domain.entity.PageTranslation;

/** One locale's overlay; every content field nullable (null = untranslated, falls back to canonical). */
public record PageTranslationResponse(
        String locale,
        String title,
        String body,
        String seoTitle,
        String seoDescription,
        String handle) {

    public static PageTranslationResponse from(PageTranslation t) {
        return new PageTranslationResponse(
                t.locale().value(),
                t.title() == null ? null : t.title().value(),
                t.body() == null ? null : t.body().value(),
                t.seoTitle() == null ? null : t.seoTitle().value(),
                t.seoDescription() == null ? null : t.seoDescription().value(),
                t.handle() == null ? null : t.handle().value());
    }
}
