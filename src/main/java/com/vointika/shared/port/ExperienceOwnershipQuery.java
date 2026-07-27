package com.vointika.shared.port;

import java.util.UUID;

/**
 * Cross-context read seam: does this experience belong to this operator?
 * Implemented by the experience context; consumed by metafield value writes
 * (the ownership-404 gate before touching custom data).
 */
public interface ExperienceOwnershipQuery {

    boolean existsForTourOperator(UUID experienceId, UUID tourOperatorId);
}
