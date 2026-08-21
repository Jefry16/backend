package com.vointika.experience.domain.entity;

import com.vointika.experience.domain.valueobject.CategoryName;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.Objects;
import java.util.UUID;

/**
 * One alternate-locale translation of a category's name. The canonical
 * {@code categories} row is the primary-locale source; this overlays a single
 * locale. {@code name} is nullable — an absent name falls back to the canonical
 * value at render time.
 */
public record CategoryTranslation(
        UUID categoryId,
        UUID tourOperatorId,
        LocaleCode locale,
        CategoryName name) {

    public CategoryTranslation {
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(tourOperatorId, "tourOperatorId");
        Objects.requireNonNull(locale, "locale");
    }

    /** An untranslated locale — name absent (full fallback), for the admin editor form. */
    public static CategoryTranslation empty(UUID categoryId, UUID tourOperatorId, LocaleCode locale) {
        return new CategoryTranslation(categoryId, tourOperatorId, locale, null);
    }

    /**
     * Nothing is translated, so this overlay changes nothing — the read falls
     * back to the canonical category. Storing such a row is indistinguishable
     * from having no row at all, except that it shows up in the translations
     * list as if a locale had been worked on. The upsert deletes instead of
     * writing one; this is the predicate it asks.
     *
     * <p>Add a translatable component to this record and it belongs here too.
     */
    public boolean isEmpty() {
        return name == null;
    }
}
