package com.vointika.touroperator.presentation.response;

import com.vointika.touroperator.application.dto.output.PolicyView;

import java.time.Instant;

/**
 * One store policy. A settings sub-resource keyed by its type, so no
 * {@code id}/{@code context} envelope (PATTERNS §4a) — the shape
 * {@code OperatorTranslationResponse} uses, for the same reason: the type is the
 * key, and the operator is the entity.
 */
public record PolicyResponse(
        String type,
        String title,
        String body,
        Instant createdAt,
        Instant updatedAt) {

    public static PolicyResponse from(PolicyView view) {
        return new PolicyResponse(view.type(), view.title(), view.body(),
                view.createdAt(), view.updatedAt());
    }
}
