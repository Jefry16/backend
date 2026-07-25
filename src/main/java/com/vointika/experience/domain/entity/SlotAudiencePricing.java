package com.vointika.experience.domain.entity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Per-slot, per-audience pricing row. price + capacity are FROZEN at slot-create
 * (the operator's per-departure commercial decision); audienceName + paxPerUnit
 * are SNAPSHOTTED from the audience but kept in sync if the audience is later
 * renamed / its pax-per-unit changed (via SlotAudienceSnapshotPropagator).
 * {@code audienceId} is intentionally NOT a foreign key, so the row outlives its
 * audience. {@code bookedCount} is mutated as bookings arrive (0 until the
 * booking context exists).
 */
public class SlotAudiencePricing {

    private final UUID id;
    private final UUID slotId;
    private final UUID audienceId;
    private final String audienceName;
    private final BigDecimal price;
    private final int capacity;
    private final int paxPerUnit;
    private final int bookedCount;

    public SlotAudiencePricing(UUID id,
                               UUID slotId,
                               UUID audienceId,
                               String audienceName,
                               BigDecimal price,
                               int capacity,
                               int paxPerUnit) {
        this(id, slotId, audienceId, audienceName, price, capacity, paxPerUnit, 0);
    }

    public SlotAudiencePricing(UUID id,
                               UUID slotId,
                               UUID audienceId,
                               String audienceName,
                               BigDecimal price,
                               int capacity,
                               int paxPerUnit,
                               int bookedCount) {
        this.id = id;
        this.slotId = slotId;
        this.audienceId = audienceId;
        this.audienceName = audienceName;
        this.price = price;
        this.capacity = capacity;
        this.paxPerUnit = paxPerUnit;
        this.bookedCount = bookedCount;
    }

    /** A repriced/recapacitied copy (same id/slot/audience/snapshot, new price/capacity). */
    public SlotAudiencePricing withCapacity(int newCapacity) {
        return new SlotAudiencePricing(id, slotId, audienceId, audienceName,
                price, newCapacity, paxPerUnit, bookedCount);
    }

    public UUID id() { return id; }
    public UUID slotId() { return slotId; }
    public UUID audienceId() { return audienceId; }
    public String audienceName() { return audienceName; }
    public BigDecimal price() { return price; }
    public int capacity() { return capacity; }
    public int paxPerUnit() { return paxPerUnit; }
    public int bookedCount() { return bookedCount; }
}
