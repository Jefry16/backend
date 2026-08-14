package com.vointika.touroperator.presentation.request;

import com.vointika.touroperator.application.dto.input.AddressInput;

import java.util.UUID;

public record CreateTourOperatorRequest(
        String name,
        AddressInput address,
        UUID timezoneId,
        UUID currencyId) {
}
