package com.vointika.metafield.presentation.request;

/** ownerType/namespace/key/type are immutable after creation. */
public record CreateMetafieldDefinitionRequest(
        String ownerType,
        String namespace,
        String key,
        String type,
        String name,
        String description) {
}
