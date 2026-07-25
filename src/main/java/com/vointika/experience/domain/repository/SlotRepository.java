package com.vointika.experience.domain.repository;

import com.vointika.experience.domain.entity.Slot;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;

import java.util.Optional;
import java.util.UUID;

public interface SlotRepository {

    Slot save(Slot slot);

    /** Tenant-scoped lookup — an id under a different operator resolves empty. */
    Optional<Slot> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    /** The operator's slots across all experiences, cursor-paginated + filtered. */
    CursorPage<Slot> list(ListQuery query);
}
