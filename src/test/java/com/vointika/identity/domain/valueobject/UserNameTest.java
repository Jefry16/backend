package com.vointika.identity.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserNameTest {

    @Test
    void shouldCreateValidName() {
        UserName name = new UserName("John");
        assertEquals("John", name.value());
    }

    @Test
    void shouldTrimWhitespace() {
        UserName name = new UserName("  John  ");
        assertEquals("John", name.value());
    }

    @Test
    void shouldThrowWhenNull() {
        InvalidFieldException ex = assertThrows(InvalidFieldException.class, () -> new UserName(null));
        assertEquals("Name cannot be blank", ex.getMessage());
    }

    @Test
    void shouldThrowWhenBlank() {
        InvalidFieldException ex = assertThrows(InvalidFieldException.class, () -> new UserName("   "));
        assertEquals("Name cannot be blank", ex.getMessage());
    }

    @Test
    void shouldThrowWhenTooShort() {
        InvalidFieldException ex = assertThrows(InvalidFieldException.class, () -> new UserName("A"));
        assertEquals("Name must be between 2 and 100 characters", ex.getMessage());
    }

    @Test
    void shouldThrowWhenTooLong() {
        String longName = "A".repeat(101);
        InvalidFieldException ex = assertThrows(InvalidFieldException.class, () -> new UserName(longName));
        assertEquals("Name must be between 2 and 100 characters", ex.getMessage());
    }

    @Test
    void shouldAcceptExactly2Characters() {
        assertDoesNotThrow(() -> new UserName("Jo"));
    }

    @Test
    void shouldAcceptExactly100Characters() {
        String name = "A".repeat(100);
        assertDoesNotThrow(() -> new UserName(name));
    }
}
