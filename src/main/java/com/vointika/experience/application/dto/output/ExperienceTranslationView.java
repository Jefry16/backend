package com.vointika.experience.application.dto.output;

import com.vointika.experience.domain.entity.ExperienceTranslation;

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
        String handle) {

    public static ExperienceTranslationView from(ExperienceTranslation t) {
        return new ExperienceTranslationView(
                t.locale().value(),
                t.name() == null ? null : t.name().value(),
                t.description() == null ? null : t.description().value(),
                t.longDescription() == null ? null : t.longDescription().value(),
                t.handle() == null ? null : t.handle().value());
    }
}
