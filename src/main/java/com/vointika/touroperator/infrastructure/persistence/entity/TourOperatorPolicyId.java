package com.vointika.touroperator.infrastructure.persistence.entity;

import com.vointika.touroperator.domain.enums.PolicyType;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite primary key for {@link TourOperatorPolicyJpaEntity}: (tourOperatorId, type). */
public class TourOperatorPolicyId implements Serializable {

    private UUID tourOperatorId;
    private PolicyType type;

    public TourOperatorPolicyId() {}

    public TourOperatorPolicyId(UUID tourOperatorId, PolicyType type) {
        this.tourOperatorId = tourOperatorId;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TourOperatorPolicyId other
                && Objects.equals(tourOperatorId, other.tourOperatorId)
                && type == other.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tourOperatorId, type);
    }
}
