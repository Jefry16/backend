package com.vointika.metafield.presentation.response;

import com.vointika.metafield.domain.projection.MetaobjectEntryListItem;

import java.time.Instant;
import java.util.UUID;

public record MetaobjectListItemResponse(
        UUID id,
        String context,
        UUID definitionId,
        String handle,
        String name,
        boolean published,
        Instant createdAt) {

    public static MetaobjectListItemResponse from(MetaobjectEntryListItem item) {
        return new MetaobjectListItemResponse(
                item.id(), "metaobjects", item.definitionId(), item.handle(),
                item.name(), item.published(), item.createdAt());
    }
}
