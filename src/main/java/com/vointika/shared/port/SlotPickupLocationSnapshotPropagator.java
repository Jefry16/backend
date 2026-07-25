package com.vointika.shared.port;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Cross-context write seam: keeps slot pickup snapshots in sync with the pickup
 * catalog (a SYNCED catalog, unlike audience pricing's frozen price/capacity).
 * Implemented by the experience context (owner of pickup_location_slot),
 * consumed by the pickup context's update/delete use cases in their transaction.
 * Booked carts/orders will snapshot the pickup separately and stay untouched.
 */
public interface SlotPickupLocationSnapshotPropagator {

    /** Pushes a rename / time change onto every slot row snapshotting this pickup. */
    void propagate(UUID pickupLocationId, String pickupLocationName, LocalTime pickupLocationTime);

    /** Removes the pickup's rows from every slot (a deleted pickup stops being offered). */
    void removeForPickupLocation(UUID pickupLocationId);
}
