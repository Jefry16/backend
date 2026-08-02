package com.vointika.experience.domain.repository;

import com.vointika.experience.domain.entity.Experience;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;

import java.util.Optional;
import java.util.UUID;

public interface ExperienceRepository {

    Experience save(Experience experience);

    /** Tenant-scoped lookup — an id under a different operator resolves empty. */
    boolean existsByIdAndTourOperatorId(UUID experienceId, UUID tourOperatorId);

    Optional<Experience> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    /** The operator's experiences, cursor-paginated + filtered. */
    CursorPage<Experience> list(ListQuery query);

    /** Whether this operator already has an experience with this handle (per-operator uniqueness). */
    boolean existsByTourOperatorIdAndHandle(UUID tourOperatorId, String handle);

    /** As above, ignoring one experience — its own canonical handle never clashes with itself. */
    boolean existsByTourOperatorIdAndHandleExcluding(UUID tourOperatorId, String handle, UUID excludeExperienceId);
}
