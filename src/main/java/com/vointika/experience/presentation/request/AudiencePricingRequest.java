package com.vointika.experience.presentation.request;

import java.math.BigDecimal;
import java.util.UUID;

/** One audience's price + capacity in a slot-create request. */
public record AudiencePricingRequest(UUID audienceId, BigDecimal price, Integer capacity) {
}
