package com.vointika.metafield.infrastructure.persistence.mapper;

import com.vointika.metafield.domain.entity.MetafieldDefinition;
import com.vointika.metafield.domain.projection.MetafieldDefinitionListItem;
import com.vointika.metafield.domain.valueobject.MetafieldDefinitionName;
import com.vointika.metafield.domain.valueobject.MetafieldDescription;
import com.vointika.metafield.domain.valueobject.MetafieldKey;
import com.vointika.metafield.domain.valueobject.MetafieldNamespace;
import com.vointika.metafield.infrastructure.persistence.entity.MetafieldDefinitionJpaEntity;

public final class MetafieldDefinitionMapper {

    private MetafieldDefinitionMapper() {}

    public static MetafieldDefinitionJpaEntity toJpa(MetafieldDefinition d) {
        return new MetafieldDefinitionJpaEntity(
                d.getId(),
                d.getTourOperatorId(),
                d.getOwnerType(),
                d.getNamespace().value(),
                d.getKey().value(),
                d.getType(),
                d.getName().value(),
                d.getDescription().map(MetafieldDescription::value).orElse(null),
                d.getCreatedBy(),
                d.getCreatedAt(),
                d.getUpdatedAt());
    }

    public static MetafieldDefinition toDomain(MetafieldDefinitionJpaEntity e) {
        return new MetafieldDefinition(
                e.getId(),
                e.getTourOperatorId(),
                e.getOwnerType(),
                new MetafieldNamespace(e.getNamespace()),
                new MetafieldKey(e.getKey()),
                e.getType(),
                new MetafieldDefinitionName(e.getName()),
                e.getDescription() == null ? null : new MetafieldDescription(e.getDescription()),
                e.getCreatedBy(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }

    public static MetafieldDefinitionListItem toListItem(MetafieldDefinitionJpaEntity e) {
        return new MetafieldDefinitionListItem(
                e.getId(),
                e.getOwnerType(),
                e.getNamespace(),
                e.getKey(),
                e.getType(),
                e.getName(),
                e.getCreatedAt());
    }
}
