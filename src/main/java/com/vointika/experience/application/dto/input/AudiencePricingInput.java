package com.vointika.experience.application.dto.input;

import java.math.BigDecimal;
import java.util.UUID;

/** One audience's price + capacity for a slot being created. */
public record AudiencePricingInput(UUID audienceId, BigDecimal price, Integer capacity) {
}
