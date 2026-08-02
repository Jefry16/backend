package com.vointika.experience.presentation.response;

import com.vointika.experience.application.dto.output.ExperienceTranslationView;

import java.util.List;

/**
 * One locale's translation overlay. {@code locale} is its identity; null content
 * fields mean untranslated (the client falls back to the canonical experience).
 */
public record ExperienceTranslationResponse(
        String locale,
        String name,
        String description,
        String longDescription,
        List<String> highlights,
        List<String> included,
        List<String> notIncluded,
        String handle) {

    public static ExperienceTranslationResponse from(ExperienceTranslationView v) {
        return new ExperienceTranslationResponse(
                v.locale(), v.name(), v.description(), v.longDescription(),
                v.highlights(), v.included(), v.notIncluded(), v.handle());
    }
}
