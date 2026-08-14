package com.vointika.metafield.domain.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * One locale's text for one metafield value.
 *
 * <p>A record rather than a mutable entity, unlike {@link MetafieldValue}: there
 * is one field and replacing it is the only operation, so there is no state
 * transition worth a method. Upsert writes a whole row or none.
 *
 * <p>{@code value} is never blank — the absence of a translation is the absence
 * of the row, which is what the storefront's {@code COALESCE} falls back through.
 */
public record MetafieldValueTranslation(UUID metafieldValueId,
                                        String locale,
                                        String value,
                                        UUID createdBy,
                                        Instant createdAt,
                                        Instant updatedAt) {

    public static MetafieldValueTranslation of(UUID metafieldValueId, String locale,
                                               String value, UUID createdBy) {
        Instant now = Instant.now();
        return new MetafieldValueTranslation(metafieldValueId, locale, value, createdBy, now, now);
    }
}
