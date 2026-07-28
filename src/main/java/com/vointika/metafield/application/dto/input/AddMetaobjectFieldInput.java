package com.vointika.metafield.application.dto.input;

import java.util.UUID;

public record AddMetaobjectFieldInput(
        UUID callerUserId,
        UUID tourOperatorId,
        UUID definitionId,
        String key,
        String type,
        String name) {
}
