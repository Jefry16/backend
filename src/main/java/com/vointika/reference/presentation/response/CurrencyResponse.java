package com.vointika.reference.presentation.response;

import java.util.UUID;

public record CurrencyResponse(
        UUID id,
        String context,
        String code,
        String name,
        String symbol
) {
    public CurrencyResponse(UUID id, String code, String name, String symbol) {
        this(id, "currencies", code, name, symbol);
    }
}
