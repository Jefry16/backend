package com.vointika.touroperator.presentation.response;

import com.vointika.touroperator.application.dto.output.PolicyTranslationView;

/** One locale's overlay on a policy; null fields fall back to the canonical text. */
public record PolicyTranslationResponse(
        String locale,
        String title,
        String body) {

    public static PolicyTranslationResponse from(PolicyTranslationView view) {
        return new PolicyTranslationResponse(view.locale(), view.title(), view.body());
    }
}
