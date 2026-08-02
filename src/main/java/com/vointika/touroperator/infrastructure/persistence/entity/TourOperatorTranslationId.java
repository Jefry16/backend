package com.vointika.touroperator.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite primary key for {@link TourOperatorTranslationJpaEntity}: (tourOperatorId, locale). */
public class TourOperatorTranslationId implements Serializable {

    private UUID tourOperatorId;
    private String locale;

    public TourOperatorTranslationId() {}

    public TourOperatorTranslationId(UUID tourOperatorId, String locale) {
        this.tourOperatorId = tourOperatorId;
        this.locale = locale;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TourOperatorTranslationId other
                && Objects.equals(tourOperatorId, other.tourOperatorId)
                && Objects.equals(locale, other.locale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tourOperatorId, locale);
    }
}
