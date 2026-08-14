package com.vointika.metafield.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite primary key for {@link MetaobjectEntryValueTranslationJpaEntity}: (entryValueId, locale). */
public class MetaobjectEntryValueTranslationId implements Serializable {

    private UUID entryValueId;
    private String locale;

    public MetaobjectEntryValueTranslationId() {}

    public MetaobjectEntryValueTranslationId(UUID entryValueId, String locale) {
        this.entryValueId = entryValueId;
        this.locale = locale;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MetaobjectEntryValueTranslationId other
                && Objects.equals(entryValueId, other.entryValueId)
                && Objects.equals(locale, other.locale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entryValueId, locale);
    }
}
