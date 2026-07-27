package com.vointika.page.domain.entity;

import com.vointika.page.domain.valueobject.PageBody;
import com.vointika.page.domain.valueobject.PageSeoDescription;
import com.vointika.page.domain.valueobject.PageSeoTitle;
import com.vointika.page.domain.valueobject.PageTitle;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.shared.valueobject.Slug;

import java.util.Objects;
import java.util.UUID;

/**
 * One locale's overlay of a page's content — the experience-translations
 * shape: every content field nullable ({@code null} = untranslated, falls back
 * per-field to canonical at render time), plus an optional per-locale
 * localized handle ({@code slug}) for the localized {@code /pages/{handle}}
 * URL. Unlike the prose fields the localized handle has no per-field fallback
 * — its absence means the canonical handle serves the locale.
 */
public record PageTranslation(
        UUID pageId,
        UUID tourOperatorId,
        LocaleCode locale,
        PageTitle title,
        PageBody body,
        PageSeoTitle seoTitle,
        PageSeoDescription seoDescription,
        Slug slug
) {
    public PageTranslation {
        Objects.requireNonNull(pageId, "pageId");
        Objects.requireNonNull(tourOperatorId, "tourOperatorId");
        Objects.requireNonNull(locale, "locale");
    }

    /** An untranslated locale — all fields absent (full fallback), for the admin editor form. */
    public static PageTranslation empty(UUID pageId, UUID tourOperatorId, LocaleCode locale) {
        return new PageTranslation(pageId, tourOperatorId, locale, null, null, null, null, null);
    }
}
