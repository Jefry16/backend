package com.vointika.touroperator.application.dto.output;

import com.vointika.touroperator.domain.entity.TourOperatorTranslation;

/** One locale's operator overlay, flattened to primitives for the wire. */
public record OperatorTranslationView(
        String locale,
        String seoTitle,
        String seoDescription,
        String passwordMessage,
        String slogan,
        String shortDescription) {

    public static OperatorTranslationView from(TourOperatorTranslation t) {
        return new OperatorTranslationView(
                t.locale().value(),
                t.seoTitle() == null ? null : t.seoTitle().value(),
                t.seoDescription() == null ? null : t.seoDescription().value(),
                t.passwordMessage(),
                t.slogan() == null ? null : t.slogan().value(),
                t.shortDescription() == null ? null : t.shortDescription().value());
    }
}
