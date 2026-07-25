package com.vointika.experience.application.dto.output;

import com.vointika.experience.domain.entity.Slot;
import com.vointika.experience.domain.entity.SlotAudiencePricing;
import com.vointika.experience.domain.valueobject.SlotStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * A slot for read APIs — the departure plus its per-audience pricing. Duration is
 * DERIVED (endAt − startAt), never stored. Times are operator-local wall-clock.
 */
public record SlotView(
        UUID id,
        UUID experienceId,
        LocalDateTime startAt,
        LocalDateTime endAt,
        int day,
        long durationMinutes,
        String experienceName,
        String experienceDescription,
        SlotStatus status,
        Instant createdAt,
        List<AudiencePricingItem> audiencePrices) {

    public record AudiencePricingItem(
            UUID audienceId,
            String audienceName,
            BigDecimal price,
            int capacity,
            int paxPerUnit,
            int bookedCount) {}

    public static SlotView from(Slot slot, List<SlotAudiencePricing> pricing) {
        return new SlotView(
                slot.id(),
                slot.experienceId(),
                slot.startAt(),
                slot.endAt(),
                slot.day(),
                Duration.between(slot.startAt(), slot.endAt()).toMinutes(),
                slot.experienceName(),
                slot.experienceDescription(),
                slot.status(),
                slot.createdAt(),
                pricing.stream()
                        .map(p -> new AudiencePricingItem(
                                p.audienceId(), p.audienceName(), p.price(),
                                p.capacity(), p.paxPerUnit(), p.bookedCount()))
                        .toList());
    }
}
