package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.infrastructure.persistence.entity.MenuJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface MenuJpaRepository extends JpaRepository<MenuJpaEntity, UUID> {

    Optional<MenuJpaEntity> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    boolean existsByTourOperatorIdAndHandle(UUID tourOperatorId, String handle);

    /** Every menu an operator has, for the storefront's globals. Handle-ordered so the payload is stable. */
    List<MenuJpaEntity> findByTourOperatorIdOrderByHandleAsc(UUID tourOperatorId);
}
