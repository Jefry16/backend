package com.vointika.touroperator.infrastructure.persistence.entity;

import com.vointika.touroperator.domain.enums.BrandColorRole;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite primary key for {@link TourOperatorBrandColorJpaEntity}: (tourOperatorId, role, position). */
public class TourOperatorBrandColorId implements Serializable {

    private UUID tourOperatorId;
    private BrandColorRole role;
    private short position;

    public TourOperatorBrandColorId() {}

    public TourOperatorBrandColorId(UUID tourOperatorId, BrandColorRole role, short position) {
        this.tourOperatorId = tourOperatorId;
        this.role = role;
        this.position = position;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TourOperatorBrandColorId other
                && Objects.equals(tourOperatorId, other.tourOperatorId)
                && role == other.role
                && position == other.position;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tourOperatorId, role, position);
    }
}
