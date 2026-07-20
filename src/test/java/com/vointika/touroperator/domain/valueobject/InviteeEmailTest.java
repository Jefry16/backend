package com.vointika.touroperator.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InviteeEmailTest {

    @Test
    void acceptsAndLowercasesAValidEmail() {
        assertEquals("ada@example.com", new InviteeEmail("Ada@Example.com").value());
        assertEquals("a.b+c@sub.example.co", new InviteeEmail("  a.b+c@sub.example.co  ").value());
    }

    @Test
    void rejectsBlank() {
        assertThrows(InvalidFieldException.class, () -> new InviteeEmail(""));
        assertThrows(InvalidFieldException.class, () -> new InviteeEmail("   "));
        assertThrows(InvalidFieldException.class, () -> new InviteeEmail(null));
    }

    @Test
    void rejectsMalformedShapes() {
        assertThrows(InvalidFieldException.class, () -> new InviteeEmail("no-at-sign.com"));
        assertThrows(InvalidFieldException.class, () -> new InviteeEmail("@example.com"));
        assertThrows(InvalidFieldException.class, () -> new InviteeEmail("a@nodot"));
        assertThrows(InvalidFieldException.class, () -> new InviteeEmail("a@.com"));
        assertThrows(InvalidFieldException.class, () -> new InviteeEmail("a@b."));
        assertThrows(InvalidFieldException.class, () -> new InviteeEmail("two@ats@example.com"));
        assertThrows(InvalidFieldException.class, () -> new InviteeEmail("has space@example.com"));
    }

    @Test
    void rejectsTooLong() {
        String local = "a".repeat(250);
        assertThrows(InvalidFieldException.class, () -> new InviteeEmail(local + "@example.com"));
    }
}
