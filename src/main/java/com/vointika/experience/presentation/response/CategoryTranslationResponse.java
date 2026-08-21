package com.vointika.experience.presentation.response;

import com.vointika.experience.domain.entity.CategoryTranslation;

/**
 * One locale's translation overlay. {@code locale} is its identity; a null
 * {@code name} means untranslated (the client falls back to the canonical name).
 */
public record CategoryTranslationResponse(String locale, String name) {

    public static CategoryTranslationResponse from(CategoryTranslation t) {
        return new CategoryTranslationResponse(
                t.locale().value(), t.name() == null ? null : t.name().value());
    }
}
