package com.vointika.reference.domain.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CountryTest {

    @Test
    void shouldExposeAllConstructorArguments() {
        UUID id = UUID.randomUUID();

        Country country = new Country(id, "ES", "Spain", "flags/es.svg");

        assertEquals(id, country.getId());
        assertEquals("ES", country.getCode());
        assertEquals("Spain", country.getName());
        assertEquals("flags/es.svg", country.getFlagKey());
    }
}
