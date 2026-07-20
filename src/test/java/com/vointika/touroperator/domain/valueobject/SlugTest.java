package com.vointika.touroperator.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SlugTest {

    @Test
    void acceptsAValidSlug() {
        assertEquals("acme-tours", new Slug("acme-tours").value());
        assertEquals("acme2", new Slug("acme2").value());
    }

    @Test
    void rejectsBlank() {
        assertThrows(InvalidFieldException.class, () -> new Slug(""));
        assertThrows(InvalidFieldException.class, () -> new Slug("   "));
        assertThrows(InvalidFieldException.class, () -> new Slug(null));
    }

    @Test
    void rejectsUppercaseAndSpaces() {
        assertThrows(InvalidFieldException.class, () -> new Slug("Acme"));
        assertThrows(InvalidFieldException.class, () -> new Slug("acme tours"));
    }

    @Test
    void rejectsLeadingOrTrailingOrDoubleDash() {
        assertThrows(InvalidFieldException.class, () -> new Slug("-acme"));
        assertThrows(InvalidFieldException.class, () -> new Slug("acme-"));
        assertThrows(InvalidFieldException.class, () -> new Slug("acme--tours"));
    }

    @Test
    void rejectsTooLong() {
        assertThrows(InvalidFieldException.class, () -> new Slug("a".repeat(171)));
    }
}
