package com.vointika.experience.presentation.request;

import java.util.List;

/** One locale's translation for an experience. All fields optional (null = untranslated). */
public record UpsertExperienceTranslationRequest(
        String name,
        String description,
        String longDescription,
        List<String> highlights,
        List<String> included,
        List<String> notIncluded,
        String slug) {
}
