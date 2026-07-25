package com.vointika.shared.port;

import java.util.UUID;

/**
 * Cross-context write seam: pushes an audience's name / pax-per-unit change onto
 * every {@code experience.audience_slot} row that snapshotted it. Implemented by
 * the experience context (owner of the slot pricing rows), consumed by the
 * audience context's update use case so those identity fields stay in sync.
 * Per-slot price + capacity are the operator's frozen decisions and are NOT
 * touched.
 */
public interface SlotAudienceSnapshotPropagator {

    void propagate(UUID audienceId, String audienceName, int paxPerUnit);
}
