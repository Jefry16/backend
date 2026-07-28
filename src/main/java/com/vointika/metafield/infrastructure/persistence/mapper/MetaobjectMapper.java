package com.vointika.metafield.infrastructure.persistence.mapper;

import com.vointika.metafield.domain.entity.MetaobjectDefinition;
import com.vointika.metafield.domain.entity.MetaobjectEntry;
import com.vointika.metafield.domain.entity.MetaobjectEntryValue;
import com.vointika.metafield.domain.entity.MetaobjectField;
import com.vointika.metafield.domain.projection.MetaobjectDefinitionListItem;
import com.vointika.metafield.domain.projection.MetaobjectEntryListItem;
import com.vointika.metafield.domain.valueobject.MetafieldDefinitionName;
import com.vointika.metafield.domain.valueobject.MetafieldDescription;
import com.vointika.metafield.domain.valueobject.MetafieldKey;
import com.vointika.metafield.domain.valueobject.MetaobjectEntryName;
import com.vointika.metafield.domain.valueobject.MetaobjectType;
import com.vointika.metafield.infrastructure.persistence.entity.MetaobjectDefinitionJpaEntity;
import com.vointika.metafield.infrastructure.persistence.entity.MetaobjectEntryJpaEntity;
import com.vointika.metafield.infrastructure.persistence.entity.MetaobjectEntryValueJpaEntity;
import com.vointika.metafield.infrastructure.persistence.entity.MetaobjectFieldJpaEntity;
import com.vointika.shared.valueobject.Slug;

public final class MetaobjectMapper {

    public static MetaobjectDefinitionJpaEntity toJpa(MetaobjectDefinition d) {
        return new MetaobjectDefinitionJpaEntity(
                d.getId(), d.getTourOperatorId(), d.getType().value(),
                d.getName().value(),
                d.getDescription().map(MetafieldDescription::value).orElse(null),
                d.getCreatedBy(), d.getCreatedAt(), d.getUpdatedAt());
    }

    public static MetaobjectDefinition toDomain(MetaobjectDefinitionJpaEntity e) {
        return new MetaobjectDefinition(
                e.getId(), e.getTourOperatorId(), new MetaobjectType(e.getType()),
                new MetafieldDefinitionName(e.getName()),
                e.getDescription() == null ? null : new MetafieldDescription(e.getDescription()),
                e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt());
    }

    public static MetaobjectDefinitionListItem toListItem(MetaobjectDefinitionJpaEntity e) {
        return new MetaobjectDefinitionListItem(
                e.getId(), new MetaobjectType(e.getType()), e.getName(), e.getCreatedAt());
    }

    public static MetaobjectFieldJpaEntity toJpa(MetaobjectField f) {
        return new MetaobjectFieldJpaEntity(
                f.getId(), f.getDefinitionId(), f.getKey().value(), f.getType(),
                f.getName().value(), f.getPosition(), f.getCreatedAt(), f.getUpdatedAt());
    }

    public static MetaobjectField toDomain(MetaobjectFieldJpaEntity e) {
        return new MetaobjectField(
                e.getId(), e.getDefinitionId(), new MetafieldKey(e.getKey()), e.getType(),
                new MetafieldDefinitionName(e.getName()), e.getPosition(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    public static MetaobjectEntryJpaEntity toJpa(MetaobjectEntry entry) {
        return new MetaobjectEntryJpaEntity(
                entry.getId(), entry.getTourOperatorId(), entry.getDefinitionId(),
                entry.getHandle().value(), entry.getName().value(), entry.isPublished(),
                entry.getCreatedBy(), entry.getCreatedAt(), entry.getUpdatedAt());
    }

    public static MetaobjectEntry toDomain(MetaobjectEntryJpaEntity e) {
        return new MetaobjectEntry(
                e.getId(), e.getTourOperatorId(), e.getDefinitionId(),
                new Slug(e.getHandle()), new MetaobjectEntryName(e.getName()), e.isPublished(),
                e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt());
    }

    public static MetaobjectEntryListItem toEntryListItem(MetaobjectEntryJpaEntity e) {
        return new MetaobjectEntryListItem(
                e.getId(), e.getDefinitionId(), e.getHandle(), e.getName(),
                e.isPublished(), e.getCreatedAt());
    }

    public static MetaobjectEntryValueJpaEntity toJpa(MetaobjectEntryValue v) {
        return new MetaobjectEntryValueJpaEntity(
                v.getId(), v.getEntryId(), v.getFieldDefinitionId(), v.getValue(),
                v.getCreatedBy(), v.getCreatedAt(), v.getUpdatedAt());
    }

    public static MetaobjectEntryValue toDomain(MetaobjectEntryValueJpaEntity e) {
        return new MetaobjectEntryValue(
                e.getId(), e.getEntryId(), e.getFieldDefinitionId(), e.getValue(),
                e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private MetaobjectMapper() {}
}
