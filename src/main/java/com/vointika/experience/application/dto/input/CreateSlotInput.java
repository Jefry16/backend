package com.vointika.experience.application.dto.input;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** A single slot: an explicit operator-local start + end, and per-audience pricing. */
public record CreateSlotInput(
        UUID callerUserId,
        UUID tourOperatorId,
        UUID experienceId,
        LocalDateTime startAt,
        LocalDateTime endAt,
        List<AudiencePricingInput> audiencePrices) {
}
