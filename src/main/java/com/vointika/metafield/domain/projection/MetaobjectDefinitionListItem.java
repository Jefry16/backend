package com.vointika.metafield.domain.projection;

import com.vointika.metafield.domain.valueobject.MetaobjectType;

import java.time.Instant;
import java.util.UUID;

/** A definitions-list row (fields live on the detail read). */
public record MetaobjectDefinitionListItem(
        UUID id,
        MetaobjectType type,
        String name,
        Instant createdAt) {
}
