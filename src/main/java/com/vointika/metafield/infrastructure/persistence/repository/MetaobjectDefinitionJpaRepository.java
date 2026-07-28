package com.vointika.metafield.infrastructure.persistence.repository;

import com.vointika.metafield.infrastructure.persistence.entity.MetaobjectDefinitionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MetaobjectDefinitionJpaRepository
        extends JpaRepository<MetaobjectDefinitionJpaEntity, UUID> {

    Optional<MetaobjectDefinitionJpaEntity> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    boolean existsByTourOperatorIdAndType(UUID tourOperatorId, String type);
}
