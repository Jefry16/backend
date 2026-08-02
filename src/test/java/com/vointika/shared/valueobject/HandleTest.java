package com.vointika.shared.valueobject;

import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HandleTest {

    @Test
    void acceptsAValidHandle() {
        assertEquals("acme-tours", new Handle("acme-tours").value());
        assertEquals("acme2", new Handle("acme2").value());
    }

    @Test
    void rejectsBlank() {
        assertThrows(InvalidFieldException.class, () -> new Handle(""));
        assertThrows(InvalidFieldException.class, () -> new Handle("   "));
        assertThrows(InvalidFieldException.class, () -> new Handle(null));
    }

    @Test
    void rejectsUppercaseAndSpaces() {
        assertThrows(InvalidFieldException.class, () -> new Handle("Acme"));
        assertThrows(InvalidFieldException.class, () -> new Handle("acme tours"));
    }

    @Test
    void rejectsLeadingOrTrailingOrDoubleDash() {
        assertThrows(InvalidFieldException.class, () -> new Handle("-acme"));
        assertThrows(InvalidFieldException.class, () -> new Handle("acme-"));
        assertThrows(InvalidFieldException.class, () -> new Handle("acme--tours"));
    }

    @Test
    void rejectsTooLong() {
        assertThrows(InvalidFieldException.class, () -> new Handle("a".repeat(171)));
    }
}
