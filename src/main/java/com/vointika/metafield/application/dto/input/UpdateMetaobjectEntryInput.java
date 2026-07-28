package com.vointika.metafield.application.dto.input;

import java.util.Map;
import java.util.UUID;

/**
 * PATCH semantics: a null {@code name}/{@code handle} keeps the current value;
 * a null {@code values} touches no field; inside {@code values}, a null or
 * blank entry CLEARS that field and anything else replaces it.
 */
public record UpdateMetaobjectEntryInput(
        UUID callerUserId,
        UUID tourOperatorId,
        UUID entryId,
        String name,
        String handle,
        Map<String, String> values) {
}
