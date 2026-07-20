package com.vointika.touroperator.presentation.request;

import java.util.UUID;

public record CreateTourOperatorRequest(
        String name,
        String address,
        UUID timezoneId,
        UUID currencyId) {
}
