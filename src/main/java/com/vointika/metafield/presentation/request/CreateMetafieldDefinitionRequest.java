package com.vointika.metafield.presentation.request;

import java.util.UUID;

/**
 * ownerType/namespace/key/type are immutable after creation.
 * {@code metaobjectDefinitionId} is required iff type is
 * {@code metaobject_reference} (the pinned metaobject type) and must be
 * absent otherwise.
 */
public record CreateMetafieldDefinitionRequest(
        String ownerType,
        String namespace,
        String key,
        String type,
        UUID metaobjectDefinitionId,
        String name,
        String description) {
}
