package com.vointika.experience.infrastructure.persistence.repository;

import com.vointika.experience.infrastructure.persistence.entity.ExperienceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExperienceJpaRepository extends JpaRepository<ExperienceJpaEntity, UUID> {

    Optional<ExperienceJpaEntity> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    boolean existsByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    boolean existsByTourOperatorIdAndSlug(UUID tourOperatorId, String slug);

    /** Navigation: resolve many ids to published experiences at once. */
    java.util.List<ExperienceJpaEntity> findByIdInAndTourOperatorIdAndPublishedTrue(
            java.util.Collection<UUID> ids, UUID tourOperatorId);

    /** Storefront read: published only, never drafts. */
    Optional<ExperienceJpaEntity> findByTourOperatorIdAndSlugAndPublishedTrue(UUID tourOperatorId, String slug);
}
