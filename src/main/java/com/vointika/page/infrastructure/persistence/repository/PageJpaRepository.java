package com.vointika.page.infrastructure.persistence.repository;

import com.vointika.page.infrastructure.persistence.entity.PageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PageJpaRepository extends JpaRepository<PageJpaEntity, UUID> {

    Optional<PageJpaEntity> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    boolean existsByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    boolean existsByTourOperatorIdAndHandle(UUID tourOperatorId, String handle);

    boolean existsByTourOperatorIdAndHandleAndIdNot(UUID tourOperatorId, String handle, UUID excludeId);
}
