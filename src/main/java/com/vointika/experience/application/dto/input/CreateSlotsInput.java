package com.vointika.experience.application.dto.input;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Recurring slots: for each weekday in {@code days} (0–6 Sunday-first) within
 * [{@code validFrom}, {@code validTo}], a slot at {@code startTime}–{@code endTime}
 * (operator-local; an end ≤ start rolls to the next day). Same per-audience
 * pricing on every generated slot.
 */
public record CreateSlotsInput(
        UUID callerUserId,
        UUID tourOperatorId,
        UUID experienceId,
        List<Integer> days,
        LocalTime startTime,
        LocalTime endTime,
        LocalDate validFrom,
        LocalDate validTo,
        List<AudiencePricingInput> audiencePrices) {
}
