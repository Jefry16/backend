package com.vointika.reference.presentation.response;

import java.util.UUID;

public record CountryResponse(
        UUID id,
        String context,
        String code,
        String name,
        String flagUrl
) {
    public CountryResponse(UUID id, String code, String name, String flagUrl) {
        this(id, "countries", code, name, flagUrl);
    }
}
