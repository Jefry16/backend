package com.vointika.pickup.domain.entity;

import com.vointika.pickup.domain.valueobject.PickupLocationName;
import com.vointika.pickup.domain.valueobject.PickupLocationTime;

import java.time.Instant;
import java.util.UUID;

/**
 * An operator's pickup point (name + meeting time-of-day). Operator-scoped and
 * SYNCED onto slots: slot rows snapshot name/time but the catalog propagates
 * every create (backfill), rename/time change, and delete onto them — unlike
 * audience pricing, whose price/capacity stay frozen per slot.
 */
public class PickupLocation {

    private final UUID id;
    private final UUID tourOperatorId;
    private PickupLocationName name;
    private PickupLocationTime time;
    private final UUID createdBy;
    private final Instant createdAt;

    /** New pickup location. */
    public PickupLocation(UUID id, UUID tourOperatorId, PickupLocationName name,
                          PickupLocationTime time, UUID createdBy) {
        this(id, tourOperatorId, name, time, createdBy, Instant.now());
    }

    /** Reconstitution from persistence. */
    public PickupLocation(UUID id, UUID tourOperatorId, PickupLocationName name,
                          PickupLocationTime time, UUID createdBy, Instant createdAt) {
        this.id = id;
        this.tourOperatorId = tourOperatorId;
        this.name = name;
        this.time = time;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public void rename(PickupLocationName newName) {
        this.name = newName;
    }

    public void changeTime(PickupLocationTime newTime) {
        this.time = newTime;
    }

    public UUID getId() { return id; }
    public UUID getTourOperatorId() { return tourOperatorId; }
    public PickupLocationName getName() { return name; }
    public PickupLocationTime getTime() { return time; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
