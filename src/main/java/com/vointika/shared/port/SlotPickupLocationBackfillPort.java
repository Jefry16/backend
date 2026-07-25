package com.vointika.shared.port;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Cross-context write seam: links a freshly created pickup location to every
 * existing slot of the operator (one INSERT…SELECT), so new catalog entries are
 * offered on already-scheduled departures too. Implemented by the experience
 * context, consumed by the pickup context's create use case in its transaction.
 */
public interface SlotPickupLocationBackfillPort {

    void backfillForTourOperator(UUID tourOperatorId, UUID pickupLocationId,
                                 String pickupLocationName, LocalTime pickupLocationTime);
}
