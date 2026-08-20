package com.vointika.reference.domain.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LanguageTest {

    @Test
    void shouldExposeAllConstructorArguments() {
        UUID id = UUID.randomUUID();

        Language language = new Language(id, "en", "English");

        assertEquals(id, language.getId());
        assertEquals("en", language.getCode());
        assertEquals("English", language.getName());
    }
}
