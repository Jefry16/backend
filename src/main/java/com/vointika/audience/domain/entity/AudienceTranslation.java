package com.vointika.audience.domain.entity;

import com.vointika.audience.domain.valueobject.AudienceName;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.Objects;
import java.util.UUID;

/**
 * One alternate-locale translation of an audience tier's display name. The
 * canonical {@code audiences} row is the primary-locale source; this overlays a
 * single locale. {@code name} is nullable — an absent name falls back to the
 * canonical value at render time.
 */
public record AudienceTranslation(
        UUID audienceId,
        UUID tourOperatorId,
        LocaleCode locale,
        AudienceName name) {

    public AudienceTranslation {
        Objects.requireNonNull(audienceId, "audienceId");
        Objects.requireNonNull(tourOperatorId, "tourOperatorId");
        Objects.requireNonNull(locale, "locale");
    }

    /** An untranslated locale — name absent (full fallback), for the admin editor form. */
    public static AudienceTranslation empty(UUID audienceId, UUID tourOperatorId, LocaleCode locale) {
        return new AudienceTranslation(audienceId, tourOperatorId, locale, null);
    }

    /**
     * Nothing is translated, so this overlay changes nothing — the read falls
     * back to the canonical audience. Storing such a row is indistinguishable
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
