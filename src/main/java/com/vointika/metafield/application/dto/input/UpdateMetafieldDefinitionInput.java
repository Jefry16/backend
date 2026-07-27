package com.vointika.metafield.application.dto.input;

import java.util.UUID;

public record UpdateMetafieldDefinitionInput(
        UUID callerUserId,
        UUID tourOperatorId,
        UUID definitionId,
        String name,
        String description) {
}
