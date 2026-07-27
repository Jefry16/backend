package com.vointika.shared.port;

import java.util.UUID;

/**
 * Cross-context read seam: does this page belong to this operator?
 * Implemented by the page context; consumed by metafield value writes.
 */
public interface PageOwnershipQuery {

    boolean existsForTourOperator(UUID pageId, UUID tourOperatorId);
}
