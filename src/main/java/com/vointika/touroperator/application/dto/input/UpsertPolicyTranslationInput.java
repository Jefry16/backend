package com.vointika.touroperator.application.dto.input;

/**
 * A policy's translated text for one locale. Both fields are optional — a null
 * (or blank) field means "not translated, fall back to the canonical policy".
 */
public record UpsertPolicyTranslationInput(String title, String body) {
}
