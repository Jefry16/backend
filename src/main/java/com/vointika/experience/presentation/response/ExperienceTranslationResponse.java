package com.vointika.experience.presentation.response;

import com.vointika.experience.application.dto.output.ExperienceTranslationView;


/**
 * One locale's translation overlay. {@code locale} is its identity; null content
 * fields mean untranslated (the client falls back to the canonical experience).
 */
public record ExperienceTranslationResponse(
        String locale,
        String name,
        String description,
        String longDescription,
        String handle,
        String seoTitle,
        String seoDescription) {

    public static ExperienceTranslationResponse from(ExperienceTranslationView v) {
        return new ExperienceTranslationResponse(
                v.locale(), v.name(), v.description(), v.longDescription(),
                v.handle(), v.seoTitle(), v.seoDescription());
    }
}
