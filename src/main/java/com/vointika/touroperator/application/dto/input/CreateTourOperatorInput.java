package com.vointika.touroperator.application.dto.input;

import java.util.UUID;

public record CreateTourOperatorInput(
        UUID userId,
        String name,
        AddressInput address,
        UUID timezoneId,
        UUID currencyId) {
}
