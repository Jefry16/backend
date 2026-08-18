package com.vointika.experience.domain.repository;

import com.vointika.experience.domain.entity.Experience;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.port.ExperienceOwnershipQuery;

import java.util.Optional;
import java.util.UUID;

public interface ExperienceRepository {

    Experience save(Experience experience);

    /** Tenant-scoped lookup — an id under a different operator resolves empty. */
    boolean existsByIdAndTourOperatorId(UUID experienceId, UUID tourOperatorId);

    Optional<Experience> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    /** The tenant-scoped read, for the callers that go on to use the experience. */
    default Experience requireByIdAndTourOperatorId(UUID id, UUID tourOperatorId) {
        return findByIdAndTourOperatorId(id, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException(ExperienceOwnershipQuery.NOT_FOUND));
    }

    /**
     * The same 404, for callers that only need the experience to exist — the
     * translation endpoints, which then work entirely off the overlay table.
     * Reading the whole aggregate to answer a boolean is what they did before.
     */
    default void requireExists(UUID id, UUID tourOperatorId) {
        if (!existsByIdAndTourOperatorId(id, tourOperatorId)) {
            throw new ResourceNotFoundException(ExperienceOwnershipQuery.NOT_FOUND);
        }
    }

    /** The operator's experiences, cursor-paginated + filtered. */
    CursorPage<Experience> list(ListQuery query);

    /** Whether this operator already has an experience with this handle (per-operator uniqueness). */
    boolean existsByTourOperatorIdAndHandle(UUID tourOperatorId, String handle);

    /** As above, ignoring one experience — its own canonical handle never clashes with itself. */
    boolean existsByTourOperatorIdAndHandleExcluding(UUID tourOperatorId, String handle, UUID excludeExperienceId);
}
