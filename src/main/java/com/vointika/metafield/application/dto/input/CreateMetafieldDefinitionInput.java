package com.vointika.metafield.application.dto.input;

import java.util.UUID;

public record CreateMetafieldDefinitionInput(
        UUID callerUserId,
        UUID tourOperatorId,
        String ownerType,
        String namespace,
        String key,
        String type,
        String name,
        String description) {
}
