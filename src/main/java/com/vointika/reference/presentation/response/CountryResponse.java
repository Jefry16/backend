package com.vointika.reference.presentation.response;

import java.util.UUID;

public record CountryResponse(
        UUID id,
        String type,
        String code,
        String name
) {
    public CountryResponse(UUID id, String code, String name) {
        this(id, "countries", code, name);
    }
}
