package com.vointika.metafield.presentation.request;

import java.util.Map;

/**
 * PATCH: null name/handle keeps current; inside {@code values} a null/blank
 * entry clears the field, anything else replaces it.
 */
public record UpdateMetaobjectRequest(
        String name, String handle, Map<String, String> values) {
}
