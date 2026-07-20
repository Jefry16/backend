package com.vointika.touroperator.application.dto.input;

import java.util.UUID;

public record CreateTourOperatorInput(
        String userId,
        String name,
        String address,
        UUID timezoneId,
        UUID currencyId) {
}
