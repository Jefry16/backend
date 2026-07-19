package com.vointika.reference.domain.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimezoneTest {

    @Test
    void shouldExposeAllConstructorArguments() {
        UUID id = UUID.randomUUID();
        Country country = new Country(UUID.randomUUID(), "ES", "Spain", "flags/es.svg");

        Timezone timezone = new Timezone(id, "Europe/Madrid", "Madrid", country);

        assertEquals(id, timezone.getId());
        assertEquals("Europe/Madrid", timezone.getName());
        assertEquals("Madrid", timezone.getCityName());
        assertEquals(country, timezone.getCountry());
    }
}
