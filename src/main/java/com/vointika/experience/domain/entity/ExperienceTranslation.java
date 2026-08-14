package com.vointika.experience.domain.entity;

import com.vointika.experience.domain.valueobject.Description;
import com.vointika.experience.domain.valueobject.ExperienceName;
import com.vointika.experience.domain.valueobject.LongDescription;
import com.vointika.experience.domain.valueobject.SeoDescription;
import com.vointika.experience.domain.valueobject.SeoTitle;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.shared.valueobject.Handle;

import java.util.Objects;
import java.util.UUID;

/**
 * A per-locale overlay on an {@link Experience}. Every content field is nullable:
 * a null field falls back to the canonical experience value at render time. The
 * {@code handle} is an optional localized URL handle (null = use the canonical
 * handle). Media is shared with the canonical experience.
 */
public record ExperienceTranslation(
        UUID experienceId,
        UUID tourOperatorId,
        LocaleCode locale,
        ExperienceName name,
        Description description,
        LongDescription longDescription,
        Handle handle,
        SeoTitle seoTitle,
        SeoDescription seoDescription) {

    public ExperienceTranslation {
        Objects.requireNonNull(experienceId, "experienceId");
        Objects.requireNonNull(tourOperatorId, "tourOperatorId");
        Objects.requireNonNull(locale, "locale");
    }

    /** A fully-untranslated locale — every content field null (the admin editor form). */
    public static ExperienceTranslation empty(UUID experienceId, UUID tourOperatorId, LocaleCode locale) {
        return new ExperienceTranslation(experienceId, tourOperatorId, locale,
                null, null, null, null, null, null);
    }

    /**
     * Nothing is translated, so this overlay changes nothing — the read falls
     * back to the canonical experience for every field. Storing such a row is
     * indistinguishable from having no row at all, except that it shows up in
     * the translations list as if a locale had been worked on. The upsert
     * deletes instead of writing one; this is the predicate it asks.
     *
     * <p>The three lists are null rather than empty here: the upsert maps an
     * absent or empty array to null, so an empty list never reaches this record.
     *
     * <p>Add a translatable component to this record and it belongs here too.
     */
    public boolean isEmpty() {
        return name == null && description == null && longDescription == null
                && handle == null && seoTitle == null && seoDescription == null;
    }
}
