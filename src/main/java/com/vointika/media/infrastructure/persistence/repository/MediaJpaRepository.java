package com.vointika.media.infrastructure.persistence.repository;

import com.vointika.media.infrastructure.persistence.entity.MediaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface MediaJpaRepository extends JpaRepository<MediaJpaEntity, UUID> {

    Optional<MediaJpaEntity> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    boolean existsByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    void deleteByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    List<MediaJpaEntity> findAllByTourOperatorIdAndIdIn(UUID tourOperatorId, Set<UUID> ids);
}
