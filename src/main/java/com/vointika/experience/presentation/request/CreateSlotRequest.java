package com.vointika.experience.presentation.request;

import java.time.LocalDateTime;
import java.util.List;

/** Create one slot: explicit operator-local start + end, and per-audience pricing. */
public record CreateSlotRequest(
        LocalDateTime startAt,
        LocalDateTime endAt,
        List<AudiencePricingRequest> audiencePrices) {
}
