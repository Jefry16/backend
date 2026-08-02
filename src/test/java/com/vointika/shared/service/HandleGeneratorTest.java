package com.vointika.shared.service;

import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HandleGeneratorTest {

    private final HandleGenerator generator = new HandleGenerator();

    @Test
    void handleifiesNameLowercasingAndDashing() {
        assertEquals("acme-tours",
                generator.generateUnique("Acme Tours", s -> false).value());
    }

    @Test
    void stripsAccentsAndPunctuation() {
        assertEquals("cafe-munchen",
                generator.generateUnique("Café Münchën!!", s -> false).value());
    }

    @Test
    void appendsNumericSuffixOnCollision() {
        Set<String> taken = Set.of("acme-tours", "acme-tours-2");
        assertEquals("acme-tours-3",
                generator.generateUnique("Acme Tours", taken::contains).value());
    }

    @Test
    void rejectsNameWithNoLettersOrDigits() {
        assertThrows(InvalidFieldException.class,
                () -> generator.generateUnique("!!! ---", s -> false));
    }
}
