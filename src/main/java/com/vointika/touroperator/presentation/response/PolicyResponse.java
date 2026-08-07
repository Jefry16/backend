package com.vointika.touroperator.presentation.response;

import com.vointika.touroperator.application.dto.output.PolicyView;

import java.time.Instant;
import java.util.UUID;

/**
 * One store policy. A resource in its own right now that it is addressed by id,
 * so it carries the {@code id} + {@code context} pair the house rule requires
 * (PATTERNS §4a) — {@code context} set by the second constructor so no caller
 * passes it.
 *
 * <p>{@code type} stays on the body as ordinary data: it is the policy's
 * identity on the storefront, not its identity here.
 */
public record PolicyResponse(
        UUID id,
        String context,
        String type,
        String title,
        String body,
        Instant createdAt,
        Instant updatedAt) {

    public PolicyResponse(UUID id, String type, String title, String body,
                          Instant createdAt, Instant updatedAt) {
        this(id, "policies", type, title, body, createdAt, updatedAt);
    }

    public static PolicyResponse from(PolicyView view) {
        return new PolicyResponse(view.id(), view.type(), view.title(), view.body(),
                view.createdAt(), view.updatedAt());
    }
}
