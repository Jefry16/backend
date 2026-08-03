package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TourOperatorJpaRepository extends JpaRepository<TourOperatorJpaEntity, UUID> {
    boolean existsByHandle(String handle);

    Optional<TourOperatorJpaEntity> findByHandle(String handle);

    boolean existsByCreatedByAndNameIgnoreCase(UUID createdBy, String name);

    List<TourOperatorJpaEntity> findByIdIn(Collection<UUID> ids);
}
