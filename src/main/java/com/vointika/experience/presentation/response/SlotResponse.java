package com.vointika.experience.presentation.response;

import com.vointika.experience.application.dto.output.SlotView;
import com.vointika.experience.domain.valueobject.SlotStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * A slot for read APIs. {@code id} + {@code context:"slots"} per the house rule.
 * Times are operator-local wall-clock; {@code durationMinutes} is derived.
 */
public record SlotResponse(
        UUID id,
        String context,
        UUID experienceId,
        String experienceName,
        LocalDateTime startAt,
        LocalDateTime endAt,
        int day,
        long durationMinutes,
        SlotStatus status,
        List<AudiencePricingResponse> audiencePrices) {

    public record AudiencePricingResponse(
            UUID audienceId,
            String audienceName,
            BigDecimal price,
            int capacity,
            int paxPerUnit,
            int bookedCount) {}

    public static SlotResponse from(SlotView v) {
        return new SlotResponse(
                v.id(), "slots", v.experienceId(), v.experienceName(),
                v.startAt(), v.endAt(), v.day(), v.durationMinutes(), v.status(),
                v.audiencePrices().stream()
                        .map(p -> new AudiencePricingResponse(
                                p.audienceId(), p.audienceName(), p.price(),
                                p.capacity(), p.paxPerUnit(), p.bookedCount()))
                        .toList());
    }
}
