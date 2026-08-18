package com.vointika.metafield.domain.repository;

import com.vointika.metafield.domain.entity.MetaobjectEntry;
import com.vointika.metafield.domain.entity.MetaobjectEntryValue;
import com.vointika.metafield.domain.projection.MetaobjectEntryListItem;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The entry AGGREGATE: the entry row plus its per-field values. */
public interface MetaobjectEntryRepository {

    /** The handle is unique per definition; both the pre-check and the index race say so. */
    String DUPLICATE_HANDLE = "A metaobject with this handle already exists for this type";

    MetaobjectEntry save(MetaobjectEntry entry);

    Optional<MetaobjectEntry> findByIdAndTourOperatorId(UUID entryId, UUID tourOperatorId);

    /**
     * The same lookup, or a 404 — the shape every caller wanted. It was written
     * out at each of them, so the message existed in as many copies as there were
     * call sites and a rename would have had to find them all.
     */
    default MetaobjectEntry requireByIdAndTourOperatorId(UUID entryId, UUID tourOperatorId) {
        return findByIdAndTourOperatorId(entryId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Metaobject not found"));
    }

    boolean existsByDefinitionIdAndHandle(UUID definitionId, String handle);

    /** The reference-metafield probe: does this entry exist under this type + operator? */
    boolean existsByIdAndDefinitionIdAndTourOperatorId(UUID entryId, UUID definitionId, UUID tourOperatorId);

    CursorPage<MetaobjectEntryListItem> list(ListQuery query);

    /** Cascades the entry's values (DB-level). */
    void delete(UUID entryId);

    MetaobjectEntryValue saveValue(MetaobjectEntryValue value);

    List<MetaobjectEntryValue> valuesOf(UUID entryId);

    Optional<MetaobjectEntryValue> findValue(UUID entryId, UUID fieldDefinitionId);

    void deleteValue(UUID valueId);
}
