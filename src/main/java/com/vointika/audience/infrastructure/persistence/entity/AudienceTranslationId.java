package com.vointika.audience.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite primary key for {@link AudienceTranslationJpaEntity}: (audienceId, locale). */
public class AudienceTranslationId implements Serializable {

    private UUID audienceId;
    private String locale;

    public AudienceTranslationId() {}

    public AudienceTranslationId(UUID audienceId, String locale) {
        this.audienceId = audienceId;
        this.locale = locale;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AudienceTranslationId other
                && Objects.equals(audienceId, other.audienceId)
                && Objects.equals(locale, other.locale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(audienceId, locale);
    }
}
