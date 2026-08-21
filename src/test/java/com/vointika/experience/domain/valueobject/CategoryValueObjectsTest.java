package com.vointika.experience.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CategoryValueObjectsTest {

    @Test
    void aNameIsTrimmed() {
        assertEquals("Boat tours", new CategoryName("  Boat tours  ").value());
    }

    @Test
    void blankAndNullAreRefused() {
        assertThrows(InvalidFieldException.class, () -> new CategoryName(null));
        assertThrows(InvalidFieldException.class, () -> new CategoryName("   "));
    }

    @Test
    void controlCharactersAreRefused() {
        assertThrows(InvalidFieldException.class, () -> new CategoryName("Boat\ntours"));
    }

    @Test
    void eightyCharactersFitAndEightyOneDoNot() {
        assertEquals(80, new CategoryName("x".repeat(80)).value().length());
        assertThrows(InvalidFieldException.class, () -> new CategoryName("x".repeat(81)));
    }

    /** The trim happens before the length check, so trailing space does not cost a character. */
    @Test
    void paddingDoesNotCountTowardTheLimit() {
        assertEquals(80, new CategoryName("  " + "x".repeat(80) + "  ").value().length());
    }
}
