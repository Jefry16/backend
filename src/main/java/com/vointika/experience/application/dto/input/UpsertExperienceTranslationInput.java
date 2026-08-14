package com.vointika.experience.application.dto.input;


/**
 * The translatable fields of an experience for one locale. Every field is
 * optional — a null (or absent) field means "not translated, fall back to
 * canonical". {@code handle} is an optional localized URL handle.
 */
public record UpsertExperienceTranslationInput(
        String name,
        String description,
        String longDescription,
        String handle,
        String seoTitle,
        String seoDescription) {
}
