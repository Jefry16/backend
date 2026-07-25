package com.vointika.experience.domain.entity;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Per-slot snapshot of one entry from the operator's pickup catalog. Written at
 * slot-creation (whole catalog) or backfill (new pickup onto existing slots);
 * kept in SYNC with the catalog (rename/time propagate, delete removes) — the
 * {@code pickupLocationId} is intentionally NOT a foreign key so the write
 * seams stay cross-context-clean.
 */
public class SlotPickupLocation {

    private final UUID id;
    private final UUID slotId;
    private final UUID pickupLocationId;
    private final String pickupLocationName;
    private final LocalTime pickupLocationTime;

    public SlotPickupLocation(UUID id,
                              UUID slotId,
                              UUID pickupLocationId,
                              String pickupLocationName,
                              LocalTime pickupLocationTime) {
        this.id = id;
        this.slotId = slotId;
        this.pickupLocationId = pickupLocationId;
        this.pickupLocationName = pickupLocationName;
        this.pickupLocationTime = pickupLocationTime;
    }

    public UUID id() { return id; }
    public UUID slotId() { return slotId; }
    public UUID pickupLocationId() { return pickupLocationId; }
    public String pickupLocationName() { return pickupLocationName; }
    public LocalTime pickupLocationTime() { return pickupLocationTime; }
}
