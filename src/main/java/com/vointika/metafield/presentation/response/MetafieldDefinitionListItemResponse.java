package com.vointika.metafield.presentation.response;

import com.vointika.metafield.domain.projection.MetafieldDefinitionListItem;

import java.time.Instant;
import java.util.UUID;

public record MetafieldDefinitionListItemResponse(
        UUID id,
        String context,
        String ownerType,
        String namespace,
        String key,
        String type,
        UUID metaobjectDefinitionId,
        String name,
        Instant createdAt) {

    public static MetafieldDefinitionListItemResponse from(MetafieldDefinitionListItem item) {
        return new MetafieldDefinitionListItemResponse(
                item.id(),
                "metafield-definitions",
                item.ownerType().code(),
                item.namespace(),
                item.key(),
                item.type().code(),
                item.metaobjectDefinitionId(),
                item.name(),
                item.createdAt());
    }
}
