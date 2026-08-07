package com.vointika.touroperator.application.dto.output;

import com.vointika.touroperator.domain.entity.Policy;

import java.time.Instant;

/** One policy, flattened to primitives for the wire. */
public record PolicyView(
        String type,
        String title,
        String body,
        Instant createdAt,
        Instant updatedAt) {

    public static PolicyView from(Policy policy) {
        return new PolicyView(
                policy.type().name(),
                policy.title().value(),
                policy.body().value(),
                policy.createdAt(),
                policy.updatedAt());
    }
}
