package com.vointika.metafield.application.dto.input;

import java.util.Map;
import java.util.UUID;

/** {@code values} maps field key → raw value; null/blank entries are skipped. */
public record CreateMetaobjectEntryInput(
        UUID callerUserId,
        UUID tourOperatorId,
        UUID definitionId,
        String handle,
        String name,
        Map<String, String> values) {
}
