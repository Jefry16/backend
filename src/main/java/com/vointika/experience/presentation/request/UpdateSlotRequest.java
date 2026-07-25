package com.vointika.experience.presentation.request;

import java.util.List;
import java.util.UUID;

/** Edit a slot: set status (AVAILABLE/SOLD_OUT) and/or per-audience capacity. */
public record UpdateSlotRequest(String status, List<TierCapacityRequest> capacities) {

    public record TierCapacityRequest(UUID audienceId, Integer capacity) {}
}
