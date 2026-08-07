package com.vointika.touroperator.application.dto.output;

import com.vointika.touroperator.domain.entity.PolicyTranslation;

/** One locale's overlay on a policy, flattened to primitives for the wire. */
public record PolicyTranslationView(
        String locale,
        String title,
        String body) {

    public static PolicyTranslationView from(PolicyTranslation t) {
        return new PolicyTranslationView(
                t.locale().value(),
                t.title() == null ? null : t.title().value(),
                t.body() == null ? null : t.body().value());
    }
}
