package com.vointika.touroperator.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The brand's two translatable strings. Both are the overlay half of a column on
 * {@code tour_operator_brand}, so their limits are that table's — a translation
 * the canonical column could not hold would be storable but not portable.
 */
class BrandTextValueObjectsTest {


    @Test
    void aSloganKeepsItsText() {
        assertEquals("Bucea con nosotros", new BrandSlogan("Bucea con nosotros").value());
    }

    @Test
    void aSloganIsTrimmed() {
        assertEquals("Bucea con nosotros", new BrandSlogan("  Bucea con nosotros  ").value());
    }

    @Test
    void aBlankSloganIsRejectedRatherThanStoredEmpty() {
        // Absence is modelled outside the type: the use case maps blank to null,
        // which is what the storefront reads as "fall back to canonical".
        assertThrows(InvalidFieldException.class, () -> new BrandSlogan(""));
        assertThrows(InvalidFieldException.class, () -> new BrandSlogan("   "));
        assertThrows(InvalidFieldException.class, () -> new BrandSlogan(null));
    }

    @Test
    void aSloganStopsAtTheColumnWidth() {
        assertEquals(80, new BrandSlogan("a".repeat(80)).value().length());
        assertThrows(InvalidFieldException.class, () -> new BrandSlogan("a".repeat(81)));
    }

    @Test
    void aSloganRejectsControlCharacters() {
        assertThrows(InvalidFieldException.class, () -> new BrandSlogan("Bu" + (char) 0 + "cea"));
        assertThrows(InvalidFieldException.class, () -> new BrandSlogan("Line\nbreak"));
    }


    @Test
    void aShortDescriptionKeepsItsText() {
        assertEquals("Salidas diarias.", new BrandShortDescription("Salidas diarias.").value());
    }

    @Test
    void aShortDescriptionIsTrimmed() {
        assertEquals("Salidas diarias.", new BrandShortDescription("  Salidas diarias.  ").value());
    }

    @Test
    void aBlankShortDescriptionIsRejectedRatherThanStoredEmpty() {
        assertThrows(InvalidFieldException.class, () -> new BrandShortDescription(""));
        assertThrows(InvalidFieldException.class, () -> new BrandShortDescription("   "));
        assertThrows(InvalidFieldException.class, () -> new BrandShortDescription(null));
    }

    @Test
    void aShortDescriptionStopsAtTheColumnWidth() {
        assertEquals(150, new BrandShortDescription("a".repeat(150)).value().length());
        assertThrows(InvalidFieldException.class, () -> new BrandShortDescription("a".repeat(151)));
    }

    @Test
    void aShortDescriptionIsNotTheSeoDescription() {
        // 150 here against the SEO description's 320: this is body copy a theme
        // renders, not a meta tag, which is why both exist rather than one.
        assertThrows(InvalidFieldException.class, () -> new BrandShortDescription("a".repeat(320)));
        assertEquals(320, new OperatorSeoDescription("a".repeat(320)).value().length());
    }
}
