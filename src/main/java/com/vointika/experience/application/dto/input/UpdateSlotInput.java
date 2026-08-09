package com.vointika.experience.application.dto.input;

import java.util.List;
import java.util.UUID;

/**
 * A slot edit: per-audience capacity. Optional; only what's present applies.
 *
 * <p>Status is deliberately not editable here. AVAILABLE and SOLD_OUT are not an
 * operator choice — a departure is full when the bookings say so, counted at
 * checkout — and CANCELLED is terminal and has its own endpoint.
 */
public record UpdateSlotInput(List<TierCapacity> capacities) {
    public record TierCapacity(UUID audienceId, Integer capacity) {}
}
