package com.vointika.metafield.domain.repository;

import com.vointika.metafield.domain.entity.MetaobjectDefinition;
import com.vointika.metafield.domain.entity.MetaobjectField;
import com.vointika.metafield.domain.projection.MetaobjectDefinitionListItem;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.UUID;

/** The definition AGGREGATE: the blueprint row plus its ordered fields. */
public interface MetaobjectDefinitionRepository {

    Supplier<ResourceNotFoundException> NOT_FOUND =
            () -> new ResourceNotFoundException("Metaobject definition not found");

    Supplier<ResourceNotFoundException> FIELD_NOT_FOUND =
            () -> new ResourceNotFoundException("Metaobject field not found");

    MetaobjectDefinition save(MetaobjectDefinition definition);

    Optional<MetaobjectDefinition> findByIdAndTourOperatorId(UUID definitionId, UUID tourOperatorId);

    /**
     * The same lookup, or a 404 — the shape every caller wanted. It was written
     * out at each of them, so the message existed in as many copies as there were
     * call sites and a rename would have had to find them all.
     *
     * <p><b>The two body-sourced ids answer differently, and this is not a rule you
     * can apply blind.</b> {@code CreateMetafieldDefinitionUseCase} looks its
     * {@code metaobjectDefinitionId} pin up by hand and answers <b>422</b>, treating
     * a bad pin as the invalid field it is. {@code CreateMetaobjectEntryUseCase}
     * calls this with a body-sourced {@code definitionId} and answers <b>404</b>.
     * Both predate the split and neither is obviously wrong; what would be wrong is
     * "fixing" one to match the other from this javadoc alone, without deciding
     * which answer the API should give.
     */
    default MetaobjectDefinition requireByIdAndTourOperatorId(UUID definitionId, UUID tourOperatorId) {
        return findByIdAndTourOperatorId(definitionId, tourOperatorId)
                .orElseThrow(NOT_FOUND);
    }

    boolean existsByTourOperatorIdAndType(UUID tourOperatorId, String type);

    CursorPage<MetaobjectDefinitionListItem> list(ListQuery query);

    /** Cascades fields, entries and values (DB-level). */
    void delete(UUID definitionId);

    MetaobjectField saveField(MetaobjectField field);

    /** The definition's fields, ordered by position. */
    List<MetaobjectField> fieldsOf(UUID definitionId);

    Optional<MetaobjectField> findField(UUID definitionId, String key);

    /** The field, or a 404 — rename and remove both open this way. */
    default MetaobjectField requireField(UUID definitionId, String key) {
        return findField(definitionId, key).orElseThrow(FIELD_NOT_FOUND);
    }

    boolean existsField(UUID definitionId, String key);

    long countFields(UUID definitionId);

    /** Cascades the field's stored values (DB-level). */
    void deleteField(UUID fieldId);
}
