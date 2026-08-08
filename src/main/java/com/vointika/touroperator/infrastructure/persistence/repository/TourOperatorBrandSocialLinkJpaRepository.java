package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorBrandSocialLinkId;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorBrandSocialLinkJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.UUID;

public interface TourOperatorBrandSocialLinkJpaRepository
        extends JpaRepository<TourOperatorBrandSocialLinkJpaEntity, TourOperatorBrandSocialLinkId> {

    /**
     * Ordered for the same reason the palette is: the links render as a list, and
     * a set has no order of its own. The platform is the only sensible key —
     * there is no operator-chosen position, and Shopify has none either.
     */
    List<TourOperatorBrandSocialLinkJpaEntity> findByTourOperatorIdOrderByPlatformAsc(UUID tourOperatorId);

    /**
     * Clears the operator's links so the write can reinsert them. The whole
     * collection is replaced rather than diffed — see
     * {@code TourOperatorBrandRepositoryImpl.save}.
     */
    @Modifying
    void deleteByTourOperatorId(UUID tourOperatorId);
}
