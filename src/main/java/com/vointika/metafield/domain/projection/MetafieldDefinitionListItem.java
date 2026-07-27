package com.vointika.metafield.domain.projection;

import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.metafield.domain.valueobject.MetafieldType;

import java.time.Instant;
import java.util.UUID;

/** A definitions-list row. */
public record MetafieldDefinitionListItem(
        UUID id,
        MetafieldOwnerType ownerType,
        String namespace,
        String key,
        MetafieldType type,
        String name,
        Instant createdAt
) {}
