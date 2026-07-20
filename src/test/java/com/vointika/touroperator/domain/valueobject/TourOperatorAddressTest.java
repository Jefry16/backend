package com.vointika.touroperator.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TourOperatorAddressTest {

    @Test
    void acceptsANormalAddress() {
        assertEquals("123 Beach Rd, Punta Cana",
                new TourOperatorAddress("123 Beach Rd, Punta Cana").value());
    }

    @Test
    void trimsSurroundingWhitespace() {
        assertEquals("123 Beach Rd", new TourOperatorAddress("  123 Beach Rd  ").value());
    }

    @Test
    void rejectsBlank() {
        assertThrows(InvalidFieldException.class, () -> new TourOperatorAddress(""));
        assertThrows(InvalidFieldException.class, () -> new TourOperatorAddress(null));
    }

    @Test
    void rejectsTooLong() {
        assertThrows(InvalidFieldException.class, () -> new TourOperatorAddress("a".repeat(501)));
    }

    @Test
    void rejectsNulCharacter() {
        assertThrows(InvalidFieldException.class, () -> new TourOperatorAddress("12" + (char) 0 + "3"));
    }
}
