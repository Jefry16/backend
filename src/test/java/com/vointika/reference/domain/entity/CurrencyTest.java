package com.vointika.reference.domain.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurrencyTest {

    @Test
    void shouldExposeAllConstructorArguments() {
        UUID id = UUID.randomUUID();

        Currency currency = new Currency(id, "EUR", "Euro", "€");

        assertEquals(id, currency.getId());
        assertEquals("EUR", currency.getCode());
        assertEquals("Euro", currency.getName());
        assertEquals("€", currency.getSymbol());
    }
}
