package com.vointika.metafield.application.dto.input;

import java.util.Map;
import java.util.UUID;

/**
 * One metaobject entry's field translations for one locale.
 *
 * @param values keyed by the <b>field key</b> alone — a metaobject field has no
 *               namespace, unlike a metafield, so there is nothing to qualify it
 *               with. A blank value clears that field's translation; a key absent
 *               from the map is left alone.
 */
public record UpsertMetaobjectFieldTranslationsInput(
        UUID callerUserId,
        UUID tourOperatorId,
        UUID metaobjectId,
        String locale,
        Map<String, String> values) {
}
