package com.vointika.touroperator.application.dto.input;

/**
 * The translatable fields of an operator for one locale. Every field is optional
 * — a null (or blank) field means "not translated, fall back to the canonical
 * operator value".
 */
public record UpsertOperatorTranslationInput(
        String seoTitle,
        String seoDescription,
        String passwordMessage) {
}
