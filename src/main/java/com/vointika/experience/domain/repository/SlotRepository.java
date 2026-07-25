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

    /**
     * Refreshes the experience name/description snapshot on every slot of the
     * experience — called when an experience edit changes either, so existing
     * departures don't keep showing stale copy.
     */
    void propagateExperienceSnapshot(UUID experienceId, String experienceName, String experienceDescription);
}
