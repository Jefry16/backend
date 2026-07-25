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

    Optional<AudienceView> findForTourOperator(UUID audienceId, UUID tourOperatorId);
}
