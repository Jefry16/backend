package com.vointika.metafield.application.dto.input;

import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;

import java.util.Map;
import java.util.UUID;

/**
 * One owner's metafield translations for one locale, in a single write.
 *
 * <p><b>The whole locale at once, not one value at a time</b> — the shape every
 * other translation editor here uses, and the shape the admin renders: one form
 * per locale. Per-value endpoints would be twelve routes per mount instead of
 * four, and one of them would collide with {@code /metafields/{namespace}/{key}}.
 *
 * @param values keyed {@code "namespace.key"}, exactly how a theme addresses one.
 *               A blank value clears that key's translation; a key simply absent
 *               from the map is left alone, so a partial save cannot silently
 *               delete what it did not mention.
 */
public record UpsertMetafieldTranslationsInput(
        UUID callerUserId,
        UUID tourOperatorId,
        MetafieldOwnerType ownerType,
        UUID ownerId,
        String locale,
        Map<String, String> values) {
}
