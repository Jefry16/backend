package com.vointika.identity.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailTest {

    @Test
    void shouldCreateValidEmail() {
        Email email = new Email("user@example.com");
        assertEquals("user@example.com", email.value());
    }

    @Test
    void shouldNormalizeToLowerCase() {
        Email email = new Email("User@Example.COM");
        assertEquals("user@example.com", email.value());
    }

    @Test
    void shouldRejectEmailWithSurroundingWhitespace() {
        assertThrows(InvalidFieldException.class, () -> new Email("  user@example.com  "));
    }

    @Test
    void shouldThrowWhenNull() {
        InvalidFieldException ex = assertThrows(InvalidFieldException.class, () -> new Email(null));
        assertEquals("Email cannot be blank", ex.getMessage());
    }

    @Test
    void shouldThrowWhenBlank() {
        InvalidFieldException ex = assertThrows(InvalidFieldException.class, () -> new Email("   "));
        assertEquals("Email cannot be blank", ex.getMessage());
    }

    @Test
    void shouldThrowWhenMissingAtSign() {
        InvalidFieldException ex = assertThrows(InvalidFieldException.class, () -> new Email("userexample.com"));
        assertEquals("Invalid email format", ex.getMessage());
    }

    @Test
    void shouldThrowWhenMissingDomain() {
        assertThrows(InvalidFieldException.class, () -> new Email("user@"));
    }

    @Test
    void shouldThrowWhenMissingLocalPart() {
        assertThrows(InvalidFieldException.class, () -> new Email("@example.com"));
    }

    @Test
    void shouldThrowWhenContainsSpaces() {
        assertThrows(InvalidFieldException.class, () -> new Email("user @example.com"));
    }
}
