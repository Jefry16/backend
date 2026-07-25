package com.vointika.audience.presentation.response;

import com.vointika.audience.domain.entity.AudienceTranslation;

/**
 * One locale's translation overlay. {@code locale} is its identity; a null
 * {@code name} means untranslated (the client falls back to the canonical name).
 */
public record AudienceTranslationResponse(String locale, String name) {

    public static AudienceTranslationResponse from(AudienceTranslation t) {
        return new AudienceTranslationResponse(
                t.locale().value(), t.name() == null ? null : t.name().value());
    }
}
