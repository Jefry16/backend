package com.vointika.experience.application.dto.input;

import java.util.List;
import java.util.UUID;

/**
 * A slot edit: set status (AVAILABLE / SOLD_OUT — not CANCELLED, use cancel)
 * and/or per-audience capacity. Both parts optional; only what's present applies.
 */
public record UpdateSlotInput(String status, List<TierCapacity> capacities) {

    public record TierCapacity(UUID audienceId, Integer capacity) {}
}
