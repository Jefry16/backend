package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.domain.enums.PolicyType;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorPolicyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TourOperatorPolicyJpaRepository
        extends JpaRepository<TourOperatorPolicyJpaEntity, UUID> {

    /**
     * The <b>storefront footer's</b> list, still a plain ordered read: that path
     * renders four links on a public page and has no cursor to carry. The admin
     * list goes through {@code CriteriaListExecutor} instead (PATTERNS §4b).
     *
     * <p>Ordered for the same reason the palette and the social links are: a set
     * has no order of its own, and a footer whose links reorder between requests
     * reads as a bug.
     */
    List<TourOperatorPolicyJpaEntity> findByTourOperatorIdOrderByTypeAsc(UUID tourOperatorId);

    Optional<TourOperatorPolicyJpaEntity> findByTourOperatorIdAndType(UUID tourOperatorId, PolicyType type);

    /** Tenant-scoped by construction: an id alone is never enough to read a policy. */
    Optional<TourOperatorPolicyJpaEntity> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    boolean existsByTourOperatorIdAndType(UUID tourOperatorId, PolicyType type);

    /**
     * Derived delete — needs its own transaction from the caller, which the use
     * case supplies through {@code TransactionRunner}. Returns the row count so
     * the domain port can answer "did one exist".
     */

}
