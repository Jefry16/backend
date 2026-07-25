package com.vointika.experience.presentation.request;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Create recurring slots over a weekday pattern + date window. */
public record CreateSlotsRequest(
        List<Integer> days,
        LocalTime startTime,
        LocalTime endTime,
        LocalDate validFrom,
        LocalDate validTo,
        List<AudiencePricingRequest> audiencePrices) {
}
