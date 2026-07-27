package com.vointika.metafield.presentation.response;

import com.vointika.metafield.domain.entity.MetafieldDefinition;
import com.vointika.metafield.domain.valueobject.MetafieldDescription;

import java.time.Instant;
import java.util.UUID;

/** {@code id} + {@code context:"metafield-definitions"} per the house rule. */
public record MetafieldDefinitionResponse(
        UUID id,
        String context,
        String ownerType,
        String namespace,
        String key,
        String type,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt) {

    public static MetafieldDefinitionResponse from(MetafieldDefinition d) {
        return new MetafieldDefinitionResponse(
                d.getId(),
                "metafield-definitions",
                d.getOwnerType().code(),
                d.getNamespace().value(),
                d.getKey().value(),
                d.getType().code(),
                d.getName().value(),
                d.getDescription().map(MetafieldDescription::value).orElse(null),
                d.getCreatedAt(),
                d.getUpdatedAt());
    }
}
