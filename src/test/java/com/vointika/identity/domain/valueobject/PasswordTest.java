package com.vointika.identity.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordTest {

    @Test
    void shouldCreateValidPassword() {
        Password password = new Password("Password1!");
        assertEquals("Password1!", password.value());
    }

    @Test
    void shouldThrowWhenNull() {
        InvalidFieldException ex = assertThrows(InvalidFieldException.class, () -> new Password(null));
        assertEquals("Password cannot be blank", ex.getMessage());
    }

    @Test
    void shouldThrowWhenBlank() {
        InvalidFieldException ex = assertThrows(InvalidFieldException.class, () -> new Password("   "));
        assertEquals("Password cannot be blank", ex.getMessage());
    }

    @Test
    void shouldThrowWhenTooShort() {
        InvalidFieldException ex = assertThrows(InvalidFieldException.class, () -> new Password("Pass1!"));
        assertEquals("Password must be at least 8 characters", ex.getMessage());
    }

    @Test
    void shouldThrowWhenNoUppercase() {
        InvalidFieldException ex = assertThrows(InvalidFieldException.class, () -> new Password("password1!"));
        assertEquals("Password must contain at least one uppercase letter", ex.getMessage());
    }

    @Test
    void shouldThrowWhenNoLowercase() {
        InvalidFieldException ex = assertThrows(InvalidFieldException.class, () -> new Password("PASSWORD1!"));
        assertEquals("Password must contain at least one lowercase letter", ex.getMessage());
    }

    @Test
    void shouldThrowWhenNoNumber() {
        InvalidFieldException ex = assertThrows(InvalidFieldException.class, () -> new Password("Password!"));
        assertEquals("Password must contain at least one number", ex.getMessage());
    }

    @Test
    void shouldThrowWhenNoSpecialCharacter() {
        InvalidFieldException ex = assertThrows(InvalidFieldException.class, () -> new Password("Password1"));
        assertEquals("Password must contain at least one special character", ex.getMessage());
    }

    @Test
    void shouldAcceptExactly8Characters() {
        assertDoesNotThrow(() -> new Password("Abcde1!x"));
    }
}
