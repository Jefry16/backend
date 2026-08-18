package com.vointika.shared.port;

import java.util.UUID;

/**
 * Cross-context read seam: does this page belong to this operator?
 * Implemented by the page context; consumed by metafield value writes.
 */
public interface PageOwnershipQuery {

    /**
     * The refusal when a page id resolves to nothing this operator owns.
     *
     * <p>On the port rather than in {@code page}, for the same reason as
     * {@link ExperienceOwnershipQuery#NOT_FOUND}: {@code metafield}'s ownership gate
     * reaches a page only through this seam, so the sentence has two contexts. It was
     * written out eleven times before it moved here.
     *
     * <p>Nothing security-critical rests on the wording — the lookups are tenant-scoped,
     * so a missing page and another operator's are already indistinguishable by
     * structure. Hence no written-once guard.
     */
    String NOT_FOUND = "Page not found";

    boolean existsForTourOperator(UUID pageId, UUID tourOperatorId);
}
