package com.vointika.metafield.domain.repository;

import com.vointika.metafield.domain.entity.MetafieldDefinition;
import com.vointika.metafield.domain.projection.MetafieldDefinitionListItem;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;

import java.util.Optional;
import java.util.UUID;

public interface MetafieldDefinitionRepository {

    MetafieldDefinition save(MetafieldDefinition definition);

    Optional<MetafieldDefinition> findByIdAndTourOperatorId(UUID definitionId, UUID tourOperatorId);

    /**
     * The same lookup, or a 404 — the shape every caller wanted. It was written
     * out at each of them, so the message existed in as many copies as there were
     * call sites and a rename would have had to find them all.
     */
    default MetafieldDefinition requireByIdAndTourOperatorId(UUID definitionId, UUID tourOperatorId) {
        return findByIdAndTourOperatorId(definitionId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Metafield definition not found"));
    }

    Optional<MetafieldDefinition> findByIdentity(
            UUID tourOperatorId, MetafieldOwnerType ownerType, String namespace, String key);

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
