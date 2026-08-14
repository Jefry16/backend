package com.vointika.metafield.domain.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * One locale's text for one metaobject entry field value.
 *
 * <p>{@link MetafieldValueTranslation}'s twin, for the same reason it is a record
 * rather than an entity: one field, and replacing it is the only operation.
 */
public record MetaobjectEntryValueTranslation(UUID entryValueId,
                                              String locale,
                                              String value,
                                              UUID createdBy,
                                              Instant createdAt,
                                              Instant updatedAt) {

    public static MetaobjectEntryValueTranslation of(UUID entryValueId, String locale,
                                                     String value, UUID createdBy) {
        Instant now = Instant.now();
        return new MetaobjectEntryValueTranslation(entryValueId, locale, value, createdBy, now, now);
    }
}
