package com.vointika.metafield.infrastructure.persistence.mapper;

import com.vointika.metafield.domain.entity.MetafieldValue;
import com.vointika.metafield.infrastructure.persistence.entity.MetafieldValueJpaEntity;

public final class MetafieldValueMapper {

    private MetafieldValueMapper() {}

    public static MetafieldValueJpaEntity toJpa(MetafieldValue v) {
        return new MetafieldValueJpaEntity(
                v.getId(), v.getDefinitionId(), v.getOwnerId(), v.getValue(),
                v.getCreatedBy(), v.getCreatedAt(), v.getUpdatedAt());
    }

    public static MetafieldValue toDomain(MetafieldValueJpaEntity e) {
        return new MetafieldValue(
                e.getId(), e.getDefinitionId(), e.getOwnerId(), e.getValue(),
                e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
