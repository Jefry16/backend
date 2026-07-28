package com.vointika.metafield.presentation.request;

import java.util.Map;
import java.util.UUID;

/** {@code values} maps field key → raw value; null/blank entries stay unset. */
public record CreateMetaobjectRequest(
        UUID definitionId, String handle, String name, Map<String, String> values) {
}
