package com.vointika.experience.domain.entity;

import com.vointika.experience.domain.valueobject.Description;
import com.vointika.experience.domain.valueobject.ExperienceName;
import com.vointika.experience.domain.valueobject.Highlight;
import com.vointika.experience.domain.valueobject.InclusionItem;
import com.vointika.experience.domain.valueobject.LongDescription;
import com.vointika.experience.domain.valueobject.SeoDescription;
import com.vointika.experience.domain.valueobject.SeoTitle;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.shared.valueobject.Slug;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A per-locale overlay on an {@link Experience}. Every content field is nullable:
 * a null field falls back to the canonical experience value at render time. The
 * {@code slug} is an optional localized URL handle (null = use the canonical
 * slug). {@code tags} are deliberately not translated (filter facets); media is
 * shared with the canonical experience.
 */
public record ExperienceTranslation(
        UUID experienceId,
        UUID tourOperatorId,
        LocaleCode locale,
        ExperienceName name,
        Description description,
        LongDescription longDescription,
        List<Highlight> highlights,
        List<InclusionItem> included,
        List<InclusionItem> notIncluded,
        Slug slug,
        SeoTitle seoTitle,
        SeoDescription seoDescription) {

    public ExperienceTranslation {
        Objects.requireNonNull(experienceId, "experienceId");
        Objects.requireNonNull(tourOperatorId, "tourOperatorId");
        Objects.requireNonNull(locale, "locale");
        highlights = highlights == null ? null : List.copyOf(highlights);
        included = included == null ? null : List.copyOf(included);
        notIncluded = notIncluded == null ? null : List.copyOf(notIncluded);
    }

    /** A fully-untranslated locale — every content field null (the admin editor form). */
    public static ExperienceTranslation empty(UUID experienceId, UUID tourOperatorId, LocaleCode locale) {
        return new ExperienceTranslation(experienceId, tourOperatorId, locale,
                null, null, null, null, null, null, null, null, null);
    }
}
