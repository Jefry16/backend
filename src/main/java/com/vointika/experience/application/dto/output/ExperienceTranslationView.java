package com.vointika.experience.application.dto.output;

import com.vointika.experience.domain.entity.ExperienceTranslation;
import com.vointika.experience.domain.valueobject.Highlight;
import com.vointika.experience.domain.valueobject.InclusionItem;

import java.util.List;

/**
 * One locale's translation overlay for read APIs. Null fields = untranslated
 * (the client falls back to the canonical experience field).
 */
public record ExperienceTranslationView(
        String locale,
        String name,
        String description,
        String longDescription,
        List<String> highlights,
        List<String> included,
        List<String> notIncluded,
        String handle) {

    public static ExperienceTranslationView from(ExperienceTranslation t) {
        return new ExperienceTranslationView(
                t.locale().value(),
                t.name() == null ? null : t.name().value(),
                t.description() == null ? null : t.description().value(),
                t.longDescription() == null ? null : t.longDescription().value(),
                t.highlights() == null ? null : t.highlights().stream().map(Highlight::value).toList(),
                t.included() == null ? null : t.included().stream().map(InclusionItem::value).toList(),
                t.notIncluded() == null ? null : t.notIncluded().stream().map(InclusionItem::value).toList(),
                t.handle() == null ? null : t.handle().value());
    }
}
