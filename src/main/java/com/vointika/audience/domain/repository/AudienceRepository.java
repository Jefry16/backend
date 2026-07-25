package com.vointika.audience.domain.repository;

import com.vointika.audience.domain.entity.Audience;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;

import java.util.Optional;
import java.util.UUID;

public interface AudienceRepository {

    Audience save(Audience audience);

    /** Tenant-scoped lookup — an id under a different operator resolves empty. */
    Optional<Audience> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    /** The operator's audiences, cursor-paginated + filtered. */
    CursorPage<Audience> list(ListQuery query);

    /** Whether this operator already has an audience with this name (per-operator uniqueness). */
    boolean existsByTourOperatorIdAndName(UUID tourOperatorId, String name);

    /** As above, ignoring one audience — lets an audience keep its own name on update. */
    boolean existsByTourOperatorIdAndNameExcluding(UUID tourOperatorId, String name, UUID excludingId);

}
