package com.vointika.touroperator.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TourOperatorNameTest {

    @Test
    void acceptsANormalNameWithSpaces() {
        assertEquals("Acme Tours", new TourOperatorName("Acme Tours").value());
    }

    @Test
    void trimsSurroundingWhitespace() {
        assertEquals("Acme Tours", new TourOperatorName("  Acme Tours  ").value());
    }

    @Test
    void rejectsBlank() {
        assertThrows(InvalidFieldException.class, () -> new TourOperatorName(""));
        assertThrows(InvalidFieldException.class, () -> new TourOperatorName("   "));
        assertThrows(InvalidFieldException.class, () -> new TourOperatorName(null));
    }

    @Test
    void rejectsTooShortOrTooLong() {
        assertThrows(InvalidFieldException.class, () -> new TourOperatorName("A"));
        assertThrows(InvalidFieldException.class, () -> new TourOperatorName("a".repeat(151)));
    }

    @Test
    void rejectsNulCharacter() {
        assertThrows(InvalidFieldException.class, () -> new TourOperatorName("Ac" + (char) 0 + "me"));
    }
}
