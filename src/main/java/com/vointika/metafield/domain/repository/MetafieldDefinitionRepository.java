package com.vointika.metafield.domain.repository;

import com.vointika.metafield.domain.entity.MetafieldDefinition;
import com.vointika.metafield.domain.projection.MetafieldDefinitionListItem;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;

import java.util.Optional;
import java.util.UUID;

public interface MetafieldDefinitionRepository {

    MetafieldDefinition save(MetafieldDefinition definition);

    Optional<MetafieldDefinition> findByIdAndTourOperatorId(UUID definitionId, UUID tourOperatorId);

    Optional<MetafieldDefinition> findByIdentity(
            UUID tourOperatorId, MetafieldOwnerType ownerType, String namespace, String key);

    boolean existsByIdentity(
            UUID tourOperatorId, MetafieldOwnerType ownerType, String namespace, String key);

    CursorPage<MetafieldDefinitionListItem> list(ListQuery query);

    void delete(UUID definitionId);
}
