package com.vointika.metafield.infrastructure.persistence.repository;

import com.vointika.metafield.infrastructure.persistence.entity.MetaobjectFieldJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MetaobjectFieldJpaRepository
        extends JpaRepository<MetaobjectFieldJpaEntity, UUID> {

    List<MetaobjectFieldJpaEntity> findByDefinitionIdOrderByPosition(UUID definitionId);

    Optional<MetaobjectFieldJpaEntity> findByDefinitionIdAndKey(UUID definitionId, String key);

    boolean existsByDefinitionIdAndKey(UUID definitionId, String key);

    long countByDefinitionId(UUID definitionId);
}
