package com.vointika.audience.infrastructure.persistence.repository;

import com.vointika.audience.infrastructure.persistence.entity.AudienceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AudienceJpaRepository extends JpaRepository<AudienceJpaEntity, UUID> {

    Optional<AudienceJpaEntity> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    boolean existsByTourOperatorIdAndName(UUID tourOperatorId, String name);

    boolean existsByTourOperatorIdAndNameAndIdNot(UUID tourOperatorId, String name, UUID id);
}
