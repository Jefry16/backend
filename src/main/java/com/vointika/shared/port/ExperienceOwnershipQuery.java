package com.vointika.shared.port;

import java.util.UUID;

/**
 * Cross-context read seam: does this experience belong to this operator?
 * Implemented by the experience context; consumed by metafield value writes
 * (the ownership-404 gate before touching custom data).
 */
public interface ExperienceOwnershipQuery {

    /**
     * The refusal when an experience id resolves to nothing this operator owns.
     *
     * <p>Lives on the port rather than in {@code experience} because two contexts
     * say it: the owning context's own lookups, and {@code metafield}'s ownership
     * gate, which reaches an experience only through this seam. It was written out
     * twelve times before it moved here.
     *
     * <p><b>Unlike {@link TourOperatorMembershipCheck#TENANT_NOT_FOUND}, nothing
     * security-critical rests on the wording.</b> A missing experience and another
     * operator's are already indistinguishable because the lookups are tenant-scoped
     * — the sentence follows from that, it does not create it. So there is no
     * written-once guard here; a divergent copy would be untidy, not a leak.
     */
    String NOT_FOUND = "Experience not found";

    boolean existsForTourOperator(UUID experienceId, UUID tourOperatorId);
}
