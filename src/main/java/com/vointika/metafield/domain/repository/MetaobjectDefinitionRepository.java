package com.vointika.metafield.domain.repository;

import com.vointika.metafield.domain.entity.MetaobjectDefinition;
import com.vointika.metafield.domain.entity.MetaobjectField;
import com.vointika.metafield.domain.projection.MetaobjectDefinitionListItem;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The definition AGGREGATE: the blueprint row plus its ordered fields. */
public interface MetaobjectDefinitionRepository {

    MetaobjectDefinition save(MetaobjectDefinition definition);

    Optional<MetaobjectDefinition> findByIdAndTourOperatorId(UUID definitionId, UUID tourOperatorId);

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
