package com.vointika.shared.port;

import java.util.Optional;
import java.util.UUID;

/**
 * Cross-context read seam: resolves an audience that belongs to a given operator,
 * for the experience context to validate + snapshot at slot-creation time.
 * Implemented by the audience context. Empty when the audience doesn't exist or
 * isn't the operator's.
 */
public interface AudienceOwnershipQuery {

    /**
     * The refusal when an audience id resolves to nothing this operator owns.
     *
     * <p>On the port for the same reason as {@link ExperienceOwnershipQuery#NOT_FOUND}
     * and {@link PageOwnershipQuery#NOT_FOUND}: {@code experience} reaches an audience
     * only through this seam, and its slot pricing raises the same 404. Seven
     * occurrences before it moved here, six in {@code audience} and one in
     * {@code experience}.
     *
     * <p>Nothing security-critical rests on the wording — the lookups are tenant-scoped,
     * so a missing audience and another operator's are indistinguishable by structure.
     * No written-once guard.
     */
    String NOT_FOUND = "Audience not found";

    Optional<AudienceView> findForTourOperator(UUID audienceId, UUID tourOperatorId);
}
