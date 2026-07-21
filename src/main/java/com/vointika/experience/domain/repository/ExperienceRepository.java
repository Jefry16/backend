package com.vointika.experience.domain.repository;

import com.vointika.experience.domain.entity.Experience;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;

import java.util.Optional;
import java.util.UUID;

public interface ExperienceRepository {

    Experience save(Experience experience);

    /** Tenant-scoped lookup — an id under a different operator resolves empty. */
    Optional<Experience> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    /** The operator's experiences, cursor-paginated + filtered. */
    CursorPage<Experience> list(ListQuery query);

    /** Whether this operator already has an experience with this slug (per-operator uniqueness). */
    boolean existsByTourOperatorIdAndSlug(UUID tourOperatorId, String slug);
}
