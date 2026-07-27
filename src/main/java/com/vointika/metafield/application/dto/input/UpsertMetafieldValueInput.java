package com.vointika.metafield.application.dto.input;

import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;

import java.util.UUID;

public record UpsertMetafieldValueInput(
        UUID callerUserId,
        UUID tourOperatorId,
        MetafieldOwnerType ownerType,
        UUID ownerId,
        String namespace,
        String key,
        String value) {
}
