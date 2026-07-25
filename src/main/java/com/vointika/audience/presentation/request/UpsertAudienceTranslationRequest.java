package com.vointika.audience.presentation.request;

/** One locale's translation for an audience. Null/blank name = untranslated (fallback). */
public record UpsertAudienceTranslationRequest(String name) {
}
