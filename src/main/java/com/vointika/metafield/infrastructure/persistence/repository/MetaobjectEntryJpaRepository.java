package com.vointika.metafield.infrastructure.persistence.repository;

import com.vointika.metafield.infrastructure.persistence.entity.MetaobjectEntryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MetaobjectEntryJpaRepository
        extends JpaRepository<MetaobjectEntryJpaEntity, UUID> {

    Optional<MetaobjectEntryJpaEntity> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    boolean existsByDefinitionIdAndHandle(UUID definitionId, String handle);

    boolean existsByIdAndDefinitionIdAndTourOperatorId(UUID id, UUID definitionId, UUID tourOperatorId);
}
