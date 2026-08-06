package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.domain.enums.PolicyType;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorPolicyId;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorPolicyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TourOperatorPolicyJpaRepository
        extends JpaRepository<TourOperatorPolicyJpaEntity, TourOperatorPolicyId> {

    /**
     * The footer's list. Ordered for the same reason the palette and the social
     * links are: a set has no order of its own, and a footer whose links reorder
     * between requests reads as a bug. The type is the only key there is —
     * a policy has no operator-chosen position and Shopify gives it none.
     */
    List<TourOperatorPolicyJpaEntity> findByTourOperatorIdOrderByTypeAsc(UUID tourOperatorId);

    Optional<TourOperatorPolicyJpaEntity> findByTourOperatorIdAndType(UUID tourOperatorId, PolicyType type);
}
