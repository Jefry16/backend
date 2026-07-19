package com.vointika.reference.presentation.response;

import java.util.UUID;

public record TimezoneResponse(
        UUID id,
        String type,
        String name,
        String cityName,
        CountryResponse country
) {
    public TimezoneResponse(UUID id, String name, String cityName, CountryResponse country) {
        this(id, "timezones", name, cityName, country);
    }
}
