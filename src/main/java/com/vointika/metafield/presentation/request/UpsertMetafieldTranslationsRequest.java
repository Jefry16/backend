package com.vointika.metafield.presentation.request;

import java.util.Map;

/**
 * @param values keyed {@code "namespace.key"}. A blank value clears that key's
 *               translation; a key left out of the map is untouched, so a
 *               partial save cannot delete what it never showed the operator.
 */
public record UpsertMetafieldTranslationsRequest(Map<String, String> values) {
}
