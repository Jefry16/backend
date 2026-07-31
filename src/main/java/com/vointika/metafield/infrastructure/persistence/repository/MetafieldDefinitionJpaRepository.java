package com.vointika.metafield.infrastructure.persistence.repository;

import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.metafield.infrastructure.persistence.entity.MetafieldDefinitionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MetafieldDefinitionJpaRepository
        extends JpaRepository<MetafieldDefinitionJpaEntity, UUID> {

    Optional<MetafieldDefinitionJpaEntity> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    Optional<MetafieldDefinitionJpaEntity> findByTourOperatorIdAndOwnerTypeAndNamespaceAndKey(
            UUID tourOperatorId, MetafieldOwnerType ownerType, String namespace, String key);

    boolean existsByMetaobjectDefinitionId(UUID metaobjectDefinitionId);

    boolean existsByTourOperatorIdAndOwnerTypeAndNamespaceAndKey(
            UUID tourOperatorId, MetafieldOwnerType ownerType, String namespace, String key);
}
