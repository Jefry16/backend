package com.vointika.touroperator.infrastructure.persistence.entity;

import com.vointika.touroperator.domain.enums.PolicyType;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Composite primary key for {@link TourOperatorPolicyTranslationJpaEntity}:
 * (tourOperatorId, type, locale).
 */
public class TourOperatorPolicyTranslationId implements Serializable {

    private UUID tourOperatorId;
    private PolicyType type;
    private String locale;

    public TourOperatorPolicyTranslationId() {}

    public TourOperatorPolicyTranslationId(UUID tourOperatorId, PolicyType type, String locale) {
        this.tourOperatorId = tourOperatorId;
        this.type = type;
        this.locale = locale;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TourOperatorPolicyTranslationId other
                && Objects.equals(tourOperatorId, other.tourOperatorId)
                && type == other.type
                && Objects.equals(locale, other.locale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tourOperatorId, type, locale);
    }
}
