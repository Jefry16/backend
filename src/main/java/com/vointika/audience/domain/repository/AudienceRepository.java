package com.vointika.audience.domain.repository;

import com.vointika.audience.domain.entity.Audience;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AudienceOwnershipQuery;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;

import java.util.Optional;
import java.util.UUID;

public interface AudienceRepository {

    Audience save(Audience audience);

    /** Tenant-scoped lookup — an id under a different operator resolves empty. */
    Optional<Audience> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    /** The tenant-scoped read, for the callers that go on to use the audience. */
    default Audience requireByIdAndTourOperatorId(UUID id, UUID tourOperatorId) {
        return findByIdAndTourOperatorId(id, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException(AudienceOwnershipQuery.NOT_FOUND));
    }

    /**
     * The same 404 for callers that only need the audience to exist — the four
     * translation endpoints, which work off the overlay table afterwards.
     *
     * <p><b>It deliberately reuses the same query rather than adding an {@code exists}
     * one.</b> `experience` and `page` gained a separate existence check because their
     * aggregates carry a media list and a body; an {@code Audience} is six scalar
     * columns, so a second query and its JPA method would cost more to keep than the
     * read it saves (LAW §2.4). What is centralised here is the message, not the plan.
     */
    default void requireExists(UUID id, UUID tourOperatorId) {
        requireByIdAndTourOperatorId(id, tourOperatorId);
    }

    /** Thrown by the pre-check and by the unique-index race, which must answer identically (PATTERNS §8d). */
    String NAME_TAKEN = "An audience with this name already exists";

    /** The operator's audiences, cursor-paginated + filtered. */
    CursorPage<Audience> list(ListQuery query);

    /** Whether this operator already has an audience with this name (per-operator uniqueness). */
    boolean existsByTourOperatorIdAndName(UUID tourOperatorId, String name);

    /** As above, ignoring one audience — lets an audience keep its own name on update. */
    boolean existsByTourOperatorIdAndNameExcluding(UUID tourOperatorId, String name, UUID excludingId);

}
