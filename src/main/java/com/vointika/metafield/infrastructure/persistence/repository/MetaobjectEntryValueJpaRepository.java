package com.vointika.metafield.infrastructure.persistence.repository;

import com.vointika.metafield.infrastructure.persistence.entity.MetaobjectEntryValueJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MetaobjectEntryValueJpaRepository
        extends JpaRepository<MetaobjectEntryValueJpaEntity, UUID> {

    List<MetaobjectEntryValueJpaEntity> findByEntryId(UUID entryId);

    Optional<MetaobjectEntryValueJpaEntity> findByEntryIdAndFieldDefinitionId(
            UUID entryId, UUID fieldDefinitionId);
}
