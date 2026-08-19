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
     * <p><b>This one still reads the row, unlike its siblings in `experience` and
     * `page`, and the name does not say so.</b> Read it as "require that it exists",
     * not as "check existence cheaply": there is no {@code existsByIdAndTourOperatorId}
     * on this repository and none is wanted. Those two contexts earned a separate
     * existence query because their aggregates carry a media list and a body; an
     * {@code Audience} is six scalar columns, so a second query plus its JPA method
     * would cost more to keep true than the read it saves (LAW §2.4).
     *
     * <p>So `PATTERNS.md` §9's pair — *"check which callers use the row they just
     * loaded"* — <b>was</b> applied here; the answer was that the read is cheap enough
     * to keep. If an {@code Audience} ever grows a collection, revisit this first.
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
