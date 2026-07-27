package com.vointika.metafield.domain.repository;

import com.vointika.metafield.domain.entity.MetafieldValue;
import com.vointika.metafield.domain.projection.MetafieldValueWithDefinition;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MetafieldValueRepository {

    MetafieldValue save(MetafieldValue value);

    Optional<MetafieldValue> findByDefinitionIdAndOwnerId(UUID definitionId, UUID ownerId);

    /** All values on one resource, joined with their definitions (operator + owner-type scoped). */
    List<MetafieldValueWithDefinition> listForOwner(
            UUID tourOperatorId, MetafieldOwnerType ownerType, UUID ownerId);

    void delete(UUID valueId);
}
