package com.vointika.shared.valueobject;

import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * One record's rules, replacing {@code identity.EmailTest} and
 * {@code touroperator.InviteeEmailTest}.
 *
 * <p><b>Those two asserted opposite things about the same input.</b> identity had
 * {@code shouldRejectEmailWithSurroundingWhitespace} — {@code "  user@example.com  "}
 * must throw — while touroperator asserted the same shape is accepted and trimmed.
 * Both passed, because they were testing two records that claimed to state one rule.
 *
 * <p>{@link #surroundingWhitespaceIsTrimmedNotRejected} is where that was settled,
 * and it inverts identity's old assertion deliberately. The deciding case is the one
 * neither test could see: a person invited as {@code "  ada@x.co  "} is stored
 * trimmed, and the already-a-member guard matches that stored form — so under the old
 * split the same human input succeeded on the invitation path and 422'd on
 * registration. Trimming is also the kinder half on a credential form, where a
 * trailing space from a paste is a slip rather than a different person.
 */
class EmailTest {

    @Test
    void acceptsAndLowercasesAValidAddress() {
        assertEquals("user@example.com", new Email("user@example.com").value());
        assertEquals("user@example.com", new Email("User@Example.COM").value());
        assertEquals("a.b+c@sub.example.co", new Email("a.b+c@sub.example.co").value());
    }

    /**
     * The behaviour that changed. identity refused this and touroperator accepted it;
     * accepting is now the single answer. Flip the constructor back to validating the
     * untrimmed value and this is the test that fails.
     */
    @Test
    void surroundingWhitespaceIsTrimmedNotRejected() {
        assertEquals("user@example.com", new Email("  user@example.com  ").value());
        assertEquals("ada@example.com", new Email("\tAda@Example.com\n").value());
    }

    /** Whitespace *inside* the address is still a malformed address, not padding. */
    @Test
    void interiorWhitespaceIsStillRejected() {
        assertThrows(InvalidFieldException.class, () -> new Email("user @example.com"));
        assertThrows(InvalidFieldException.class, () -> new Email("has space@example.com"));
    }

    @Test
    void rejectsBlank() {
        assertEquals("Email cannot be blank",
                assertThrows(InvalidFieldException.class, () -> new Email(null)).getMessage());
        assertEquals("Email cannot be blank",
                assertThrows(InvalidFieldException.class, () -> new Email("")).getMessage());
        assertEquals("Email cannot be blank",
                assertThrows(InvalidFieldException.class, () -> new Email("   ")).getMessage());
    }

    @Test
    void rejectsMalformedShapes() {
        assertEquals("Invalid email format",
                assertThrows(InvalidFieldException.class, () -> new Email("userexample.com")).getMessage());
        for (String bad : new String[]{
                "@example.com", "user@", "a@nodot", "a@.com", "a@b.", "two@ats@example.com"}) {
            assertThrows(InvalidFieldException.class, () -> new Email(bad), bad);
        }
    }

    @Test
    void rejectsOverTheLengthLimit() {
        String tooLong = "a".repeat(Email.MAX_LENGTH) + "@example.com";
        assertEquals("Email must be at most " + Email.MAX_LENGTH + " characters",
                assertThrows(InvalidFieldException.class, () -> new Email(tooLong)).getMessage());
    }
}
