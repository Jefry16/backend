package com.vointika.metafield.presentation.response;

import com.vointika.metafield.domain.projection.MetafieldValueWithDefinition;

import java.time.Instant;

/** One stored value + its definition's identity (per-resource read). */
public record MetafieldValueResponse(
        String namespace,
        String key,
        String type,
        String name,
        String value,
        Instant updatedAt) {

    public static MetafieldValueResponse from(MetafieldValueWithDefinition v) {
        return new MetafieldValueResponse(
                v.namespace(), v.key(), v.type().code(), v.name(), v.value(), v.updatedAt());
    }
}
