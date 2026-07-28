package com.vointika.metafield.domain.repository;

import com.vointika.metafield.domain.entity.MetaobjectEntry;
import com.vointika.metafield.domain.entity.MetaobjectEntryValue;
import com.vointika.metafield.domain.projection.MetaobjectEntryListItem;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The entry AGGREGATE: the entry row plus its per-field values. */
public interface MetaobjectEntryRepository {

    MetaobjectEntry save(MetaobjectEntry entry);

    Optional<MetaobjectEntry> findByIdAndTourOperatorId(UUID entryId, UUID tourOperatorId);

    boolean existsByDefinitionIdAndHandle(UUID definitionId, String handle);

    CursorPage<MetaobjectEntryListItem> list(ListQuery query);

    /** Cascades the entry's values (DB-level). */
    void delete(UUID entryId);

    MetaobjectEntryValue saveValue(MetaobjectEntryValue value);

    List<MetaobjectEntryValue> valuesOf(UUID entryId);

    Optional<MetaobjectEntryValue> findValue(UUID entryId, UUID fieldDefinitionId);

    void deleteValue(UUID valueId);
}
