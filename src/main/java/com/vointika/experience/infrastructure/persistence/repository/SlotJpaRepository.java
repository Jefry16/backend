package com.vointika.experience.infrastructure.persistence.repository;

import com.vointika.experience.infrastructure.persistence.entity.SlotJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SlotJpaRepository extends JpaRepository<SlotJpaEntity, UUID> {

    Optional<SlotJpaEntity> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);
}
