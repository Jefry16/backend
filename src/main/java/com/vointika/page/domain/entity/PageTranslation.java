package com.vointika.page.domain.entity;

import com.vointika.page.domain.valueobject.PageBody;
import com.vointika.shared.valueobject.SeoDescription;
import com.vointika.shared.valueobject.SeoTitle;
import com.vointika.page.domain.valueobject.PageTitle;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.shared.valueobject.Handle;

import java.util.Objects;
import java.util.UUID;

/**
 * One locale's overlay of a page's content — the experience-translations
 * shape: every content field nullable ({@code null} = untranslated, falls back
 * per-field to canonical at render time), plus an optional per-locale
 * localized handle ({@code handle}) for the localized {@code /pages/{handle}}
 * URL. Unlike the prose fields the localized handle has no per-field fallback
 * — its absence means the canonical handle serves the locale.
 */
public record PageTranslation(
        UUID pageId,
        UUID tourOperatorId,
        LocaleCode locale,
        PageTitle title,
        PageBody body,
        SeoTitle seoTitle,
        SeoDescription seoDescription,
        Handle handle
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

    /**
     * Nothing is translated, so this overlay changes nothing — the read falls
     * back to the canonical page for every field. Storing such a row is
     * indistinguishable from having no row at all, except that it shows up in
     * the translations list as if a locale had been worked on. The upsert
     * deletes instead of writing one; this is the predicate it asks.
     *
     * <p>Add a translatable component to this record and it belongs here too.
     */
    public boolean isEmpty() {
        return title == null && body == null && seoTitle == null
                && seoDescription == null && handle == null;
    }
}
