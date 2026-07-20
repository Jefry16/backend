package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TourOperatorJpaRepository extends JpaRepository<TourOperatorJpaEntity, UUID> {
    boolean existsBySlug(String slug);

    boolean existsByCreatedByAndNameIgnoreCase(UUID createdBy, String name);

    List<TourOperatorJpaEntity> findByIdIn(Collection<UUID> ids);
}
