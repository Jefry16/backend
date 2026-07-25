package com.vointika.pickup.infrastructure.persistence.repository;

import com.vointika.pickup.infrastructure.persistence.entity.PickupLocationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PickupLocationJpaRepository extends JpaRepository<PickupLocationJpaEntity, UUID> {

    Optional<PickupLocationJpaEntity> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    List<PickupLocationJpaEntity> findAllByTourOperatorId(UUID tourOperatorId);

    boolean existsByTourOperatorIdAndNameIgnoreCase(UUID tourOperatorId, String name);

    boolean existsByTourOperatorIdAndNameIgnoreCaseAndIdNot(UUID tourOperatorId, String name, UUID id);
}
