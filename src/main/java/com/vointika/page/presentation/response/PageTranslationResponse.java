package com.vointika.page.presentation.response;

import com.vointika.page.domain.entity.PageTranslation;
import com.vointika.page.domain.valueobject.PageBody;
import com.vointika.page.domain.valueobject.PageSeoDescription;
import com.vointika.page.domain.valueobject.PageSeoTitle;
import com.vointika.page.domain.valueobject.PageTitle;
import com.vointika.shared.valueobject.Handle;

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
