package com.vointika.experience.application.dto.input;

import java.util.List;

/**
 * The translatable fields of an experience for one locale. Every field is
 * optional — a null (or absent) field means "not translated, fall back to
 * canonical". {@code handle} is an optional localized URL handle.
 */
public record UpsertExperienceTranslationInput(
        String name,
        String description,
        String longDescription,
        List<String> highlights,
        List<String> included,
        List<String> notIncluded,
        String handle,
        String seoTitle,
        String seoDescription) {
}
