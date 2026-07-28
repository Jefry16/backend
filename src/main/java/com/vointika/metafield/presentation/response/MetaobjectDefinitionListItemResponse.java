package com.vointika.metafield.presentation.response;

import com.vointika.metafield.domain.projection.MetaobjectDefinitionListItem;

import java.time.Instant;
import java.util.UUID;

public record MetaobjectDefinitionListItemResponse(
        UUID id,
        String context,
        String type,
        String name,
        Instant createdAt) {

    public static MetaobjectDefinitionListItemResponse from(MetaobjectDefinitionListItem item) {
        return new MetaobjectDefinitionListItemResponse(
                item.id(), "metaobject-definitions", item.type().value(),
                item.name(), item.createdAt());
    }
}
