package com.vointika.touroperator.presentation.response;

import com.vointika.touroperator.application.dto.output.OperatorTranslationView;

/**
 * One locale's operator overlay. A settings sub-resource keyed by its locale, so
 * no {@code id}/{@code context} envelope (PATTERNS §4a) — the same shape
 * {@code ExperienceTranslationResponse} uses.
 */
public record OperatorTranslationResponse(
        String locale,
        String seoTitle,
        String seoDescription,
        String passwordMessage) {

    public static OperatorTranslationResponse from(OperatorTranslationView view) {
        return new OperatorTranslationResponse(
                view.locale(), view.seoTitle(), view.seoDescription(), view.passwordMessage());
    }
}
