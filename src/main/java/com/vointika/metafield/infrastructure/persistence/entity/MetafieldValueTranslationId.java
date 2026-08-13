package com.vointika.metafield.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite primary key for {@link MetafieldValueTranslationJpaEntity}: (metafieldValueId, locale). */
public class MetafieldValueTranslationId implements Serializable {

    private UUID metafieldValueId;
    private String locale;

    public MetafieldValueTranslationId() {}

    public MetafieldValueTranslationId(UUID metafieldValueId, String locale) {
        this.metafieldValueId = metafieldValueId;
        this.locale = locale;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MetafieldValueTranslationId other
                && Objects.equals(metafieldValueId, other.metafieldValueId)
                && Objects.equals(locale, other.locale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metafieldValueId, locale);
    }
}
