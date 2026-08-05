package com.vointika.touroperator.infrastructure.persistence.entity;

import com.vointika.touroperator.domain.enums.BrandSocialPlatform;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite primary key for {@link TourOperatorBrandSocialLinkJpaEntity}: (tourOperatorId, platform). */
public class TourOperatorBrandSocialLinkId implements Serializable {

    private UUID tourOperatorId;
    private BrandSocialPlatform platform;

    public TourOperatorBrandSocialLinkId() {}

    public TourOperatorBrandSocialLinkId(UUID tourOperatorId, BrandSocialPlatform platform) {
        this.tourOperatorId = tourOperatorId;
        this.platform = platform;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TourOperatorBrandSocialLinkId other
                && Objects.equals(tourOperatorId, other.tourOperatorId)
                && platform == other.platform;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tourOperatorId, platform);
    }
}
