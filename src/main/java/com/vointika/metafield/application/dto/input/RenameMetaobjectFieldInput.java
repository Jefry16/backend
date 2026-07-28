package com.vointika.metafield.application.dto.input;

import java.util.UUID;

public record RenameMetaobjectFieldInput(
        UUID callerUserId,
        UUID tourOperatorId,
        UUID definitionId,
        String key,
        String name) {
}
