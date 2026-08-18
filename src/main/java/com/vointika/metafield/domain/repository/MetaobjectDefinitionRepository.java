package com.vointika.metafield.domain.repository;

import com.vointika.metafield.domain.entity.MetaobjectDefinition;
import com.vointika.metafield.domain.entity.MetaobjectField;
import com.vointika.metafield.domain.projection.MetaobjectDefinitionListItem;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The definition AGGREGATE: the blueprint row plus its ordered fields. */
public interface MetaobjectDefinitionRepository {

    MetaobjectDefinition save(MetaobjectDefinition definition);

    Optional<MetaobjectDefinition> findByIdAndTourOperatorId(UUID definitionId, UUID tourOperatorId);

    /**
     * The same lookup, or a 404 — the shape every caller wanted. It was written
     * out at each of them, so the message existed in as many copies as there were
     * call sites and a rename would have had to find them all.
     *
     * <p><b>Only where the id came from the path.</b> A definition id that arrives
     * in a <em>body</em> is a field, and a bad field is a 422 —
     * {@code CreateMetafieldDefinitionUseCase} looks the pin up by hand for exactly
     * that reason. Reaching for this there would turn a validation error into a
     * missing-resource one.
     */
    default MetaobjectDefinition requireByIdAndTourOperatorId(UUID definitionId, UUID tourOperatorId) {
        return findByIdAndTourOperatorId(definitionId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Metaobject definition not found"));
    }

    boolean existsByTourOperatorIdAndType(UUID tourOperatorId, String type);

    CursorPage<MetaobjectDefinitionListItem> list(ListQuery query);

    /** Cascades fields, entries and values (DB-level). */
    void delete(UUID definitionId);

    MetaobjectField saveField(MetaobjectField field);

    /** The definition's fields, ordered by position. */
    List<MetaobjectField> fieldsOf(UUID definitionId);

    Optional<MetaobjectField> findField(UUID definitionId, String key);

    boolean existsField(UUID definitionId, String key);

    long countFields(UUID definitionId);

    /** Cascades the field's stored values (DB-level). */
    void deleteField(UUID fieldId);
}
