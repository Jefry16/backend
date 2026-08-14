package com.vointika.metafield.infrastructure.persistence.repository;

import com.vointika.metafield.domain.entity.MetafieldValue;
import com.vointika.metafield.domain.projection.MetafieldValueWithDefinition;
import com.vointika.metafield.domain.repository.MetafieldValueRepository;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.metafield.infrastructure.persistence.mapper.MetafieldValueMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MetafieldValueRepositoryImpl implements MetafieldValueRepository {

    private final MetafieldValueJpaRepository jpa;

    public MetafieldValueRepositoryImpl(MetafieldValueJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public MetafieldValue save(MetafieldValue value) {
        return MetafieldValueMapper.toDomain(jpa.save(MetafieldValueMapper.toJpa(value)));
    }

    @Override
    public Optional<MetafieldValue> findByDefinitionIdAndOwnerId(UUID definitionId, UUID ownerId) {
        return jpa.findByDefinitionIdAndOwnerId(definitionId, ownerId)
                .map(MetafieldValueMapper::toDomain);
    }

    @Override
    public List<MetafieldValueWithDefinition> listForOwner(
            UUID tourOperatorId, MetafieldOwnerType ownerType, UUID ownerId) {
        return jpa.listForOwner(tourOperatorId, ownerType, ownerId);
    }

    @Override
    public List<MetafieldValueWithDefinition> listForOwnerLocalized(
            UUID tourOperatorId, MetafieldOwnerType ownerType, UUID ownerId, String locale) {
        return jpa.listForOwnerLocalized(tourOperatorId, ownerType, ownerId, locale);
    }

    @Override
    public void delete(UUID valueId) {
        jpa.deleteById(valueId);
    }

    @Override
    public void deleteReferencesTo(UUID entryId) {
        jpa.deleteReferencesTo(entryId.toString());
    }

    @Override
    public void deleteByOwnerId(UUID ownerId) {
        jpa.deleteByOwnerId(ownerId);
    }
}
