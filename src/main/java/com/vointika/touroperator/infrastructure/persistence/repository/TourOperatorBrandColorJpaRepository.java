package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorBrandColorId;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorBrandColorJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.UUID;

public interface TourOperatorBrandColorJpaRepository
        extends JpaRepository<TourOperatorBrandColorJpaEntity, TourOperatorBrandColorId> {

    /**
     * <b>The palette's order lives in this method name and nowhere else.</b> A
     * role is an ordered list — {@code colors.primary[0]} is a promise — and
     * without the ORDER BY the palette reorders between requests, which reads as
     * a bug rather than as a missing sort. The adapter groups by role and passes
     * the order through, so no assertion over a mocked repository can see it;
     * {@code StorefrontTourOperatorQueryImplTest} parses this name with Spring Data's
     * own {@code PartTree} instead.
     */
    List<TourOperatorBrandColorJpaEntity> findByTourOperatorIdOrderByPositionAsc(UUID tourOperatorId);

    /**
     * Clears the operator's colours so the write can reinsert them. The whole
     * collection is replaced rather than diffed — see
     * {@code TourOperatorBrandRepositoryImpl.save}.
     */
    @Modifying
    void deleteByTourOperatorId(UUID tourOperatorId);
}
