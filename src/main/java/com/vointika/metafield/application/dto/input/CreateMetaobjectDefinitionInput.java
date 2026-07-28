package com.vointika.metafield.application.dto.input;

import java.util.List;
import java.util.UUID;

public record CreateMetaobjectDefinitionInput(
        UUID callerUserId,
        UUID tourOperatorId,
        String type,
        String name,
        String description,
        List<FieldSpec> fields) {

    /** One field in creation order: key + type code + display name. */
    public record FieldSpec(String key, String type, String name) {
    }
}
