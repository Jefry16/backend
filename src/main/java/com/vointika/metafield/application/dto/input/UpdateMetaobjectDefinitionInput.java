package com.vointika.metafield.application.dto.input;

import java.util.UUID;

public record UpdateMetaobjectDefinitionInput(
        UUID callerUserId,
        UUID tourOperatorId,
        UUID definitionId,
        String name,
        String description) {
}
