package com.vointika.experience.infrastructure.persistence.repository;

import com.vointika.experience.infrastructure.persistence.entity.ExperienceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface ExperienceJpaRepository extends JpaRepository<ExperienceJpaEntity, UUID> {

    Optional<ExperienceJpaEntity> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    boolean existsByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    boolean existsByTourOperatorIdAndHandle(UUID tourOperatorId, String handle);

    boolean existsByTourOperatorIdAndHandleAndIdNot(UUID tourOperatorId, String handle, UUID excludeExperienceId);

    /**
     * The storefront's featured cards.
     *
     * <p><b>Everything about this read lives in the method name</b>, which is the
     * point: the filter (published <em>and</em> featured), the order and the cap
     * are one fact each, and a mechanical rename moves them together. A string
     * literal beside them would rename along with the method and stay green.
     *
     * <p>{@code featured DESC} is absent because the filter already fixed it.
     * {@code createdAt} then {@code id} is the tie-break, and it is load-bearing:
     * the seeded operator's two featured experiences share a {@code created_at}
     * to the microsecond on purpose, so id is what decides their order.
     */
    List<ExperienceJpaEntity> findTop12ByTourOperatorIdAndPublishedTrueAndFeaturedTrueOrderByCreatedAtAscIdAsc(
            UUID tourOperatorId);
}
