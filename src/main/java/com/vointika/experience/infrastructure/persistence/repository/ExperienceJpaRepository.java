package com.vointika.experience.infrastructure.persistence.repository;

import com.vointika.experience.infrastructure.persistence.entity.ExperienceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExperienceJpaRepository extends JpaRepository<ExperienceJpaEntity, UUID> {

    Optional<ExperienceJpaEntity> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    boolean existsByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    boolean existsByTourOperatorIdAndHandle(UUID tourOperatorId, String handle);

    boolean existsByTourOperatorIdAndHandleAndIdNot(UUID tourOperatorId, String handle, UUID excludeExperienceId);
}
