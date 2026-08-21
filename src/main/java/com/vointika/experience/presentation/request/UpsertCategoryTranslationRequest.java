package com.vointika.experience.presentation.request;

/** One locale's translation for a category. Null/blank name = untranslated (fallback). */
public record UpsertCategoryTranslationRequest(String name) {
}
