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

    /**
     * The same, with one locale's translations overlaid — the storefront's read.
     * A value with no translation in that locale falls back to the canonical one.
     */
    List<MetafieldValueWithDefinition> listForOwnerLocalized(
            UUID tourOperatorId, MetafieldOwnerType ownerType, UUID ownerId, String locale);

    void delete(UUID valueId);

    /** Clears every metaobject_reference value pointing at a deleted entry. */
    void deleteReferencesTo(UUID entryId);

    /** Clears every value a deleted owner (experience, page) held. */
    void deleteByOwnerId(UUID ownerId);
}
