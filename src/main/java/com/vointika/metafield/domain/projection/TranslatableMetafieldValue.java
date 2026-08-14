package com.vointika.metafield.domain.projection;

import com.vointika.metafield.domain.valueobject.MetafieldType;

import java.util.UUID;

/**
 * One of an owner's metafield values, addressed the way a translation payload
 * addresses it: {@code namespace.key}, plus the type that says whether it may be
 * translated and the value id a translation row hangs off.
 *
 * <p>It exists so the translation upsert can resolve a whole payload from one
 * query. Resolving per key cost two round trips each — the shape
 * {@code UpsertMetaobjectFieldTranslationsUseCase} avoided by loading its fields
 * and values up front, and this is the same fix for its twin.
 */
public record TranslatableMetafieldValue(String namespace,
                                         String key,
                                         MetafieldType type,
                                         UUID valueId) {

    /** How a translation payload names this value. */
    public String qualifiedKey() {
        return namespace + "." + key;
    }
}
