package com.vointika.experience.presentation.request;

import com.vointika.experience.application.dto.input.AudiencePricingInput;

import java.time.LocalDateTime;
import java.util.List;

/** Create one slot: explicit operator-local start + end, and per-audience pricing. */
public record CreateSlotRequest(
        LocalDateTime startAt,
        LocalDateTime endAt,
        List<AudiencePricingInput> audiencePrices) {
}
