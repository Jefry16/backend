package com.vointika.metafield.domain.repository;

import com.vointika.metafield.domain.entity.MetafieldDefinition;
import com.vointika.metafield.domain.projection.MetafieldDefinitionListItem;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.UUID;

public interface MetafieldDefinitionRepository {

    /** One message for both lookups — the two differ in how they address a row, not in what is missing. */
    Supplier<ResourceNotFoundException> NOT_FOUND =
            () -> new ResourceNotFoundException("Metafield definition not found");

    MetafieldDefinition save(MetafieldDefinition definition);

    Optional<MetafieldDefinition> findByIdAndTourOperatorId(UUID definitionId, UUID tourOperatorId);

    /**
     * The same lookup, or a 404 — the shape every caller wanted. It was written
     * out at each of them, so the message existed in as many copies as there were
     * call sites and a rename would have had to find them all.
     */
    default MetafieldDefinition requireByIdAndTourOperatorId(UUID definitionId, UUID tourOperatorId) {
        return findByIdAndTourOperatorId(definitionId, tourOperatorId).orElseThrow(NOT_FOUND);
    }

    Optional<MetafieldDefinition> findByIdentity(
            UUID tourOperatorId, MetafieldOwnerType ownerType, String namespace, String key);

    /**
     * The identity lookup, or the same 404 — a value endpoint addresses its
     * definition by {@code namespace.key} rather than by id, and answers
     * identically when there is none.
     */
    default MetafieldDefinition requireByIdentity(
            UUID tourOperatorId, MetafieldOwnerType ownerType, String namespace, String key) {
        return findByIdentity(tourOperatorId, ownerType, namespace, key).orElseThrow(NOT_FOUND);
    }

    boolean existsByIdentity(
            UUID tourOperatorId, MetafieldOwnerType ownerType, String namespace, String key);

    /**
     * Whether any reference-typed definition pins this metaobject type. The FK
     * that backs it raises SQLSTATE 23503, which Spring maps to
     * {@code DataIntegrityViolationException} — a class the transaction runner
     * deliberately does NOT translate (it is the parent of the duplicate-key
     * one), so the delete has to ask rather than catch.
     */
    boolean existsPinningMetaobjectDefinition(UUID metaobjectDefinitionId);

    CursorPage<MetafieldDefinitionListItem> list(ListQuery query);

    void delete(UUID definitionId);
}
