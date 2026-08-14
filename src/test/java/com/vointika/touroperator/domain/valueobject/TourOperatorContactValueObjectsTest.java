package com.vointika.touroperator.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TourOperatorContactValueObjectsTest {


    @Test
    void aPhoneKeepsWhateverFormatTheOperatorWrites() {
        // No E.164 rule on purpose: this is rendered as text in a footer, not
        // dialled, and V9 left the column without a CHECK for the same reason.
        assertEquals("+34 600 00 00 00", new TourOperatorPhone("+34 600 00 00 00").value());
        assertEquals("(809) 555-0100", new TourOperatorPhone("  (809) 555-0100  ").value());
        assertDoesNotThrow(() -> new TourOperatorPhone("600000000"));
    }

    @Test
    void aPhoneRejectsBlanksAndControlCharacters() {
        assertThrows(InvalidFieldException.class, () -> new TourOperatorPhone(""));
        assertThrows(InvalidFieldException.class, () -> new TourOperatorPhone("   "));
        assertThrows(InvalidFieldException.class, () -> new TourOperatorPhone(null));
        assertThrows(InvalidFieldException.class, () -> new TourOperatorPhone("600\n000"));
    }

    @Test
    void aPhoneStopsAtTheColumnWidth() {
        assertEquals(30, new TourOperatorPhone("9".repeat(30)).value().length());
        assertThrows(InvalidFieldException.class, () -> new TourOperatorPhone("9".repeat(31)));
    }


    @Test
    void anEmailIsLowerCasedAndTrimmed() {
        // Locale.ROOT matters: a Turkish default folds I to a dotless i.
        assertEquals("hola@acme.test", new TourOperatorEmail("  HOLA@Acme.TEST  ").value());
    }

    @Test
    void anEmailIsShapeCheckedLooselyNotGrammatically() {
        assertDoesNotThrow(() -> new TourOperatorEmail("a+tag@sub.example.co.uk"));
        assertThrows(InvalidFieldException.class, () -> new TourOperatorEmail("hola"));
        assertThrows(InvalidFieldException.class, () -> new TourOperatorEmail("@acme.test"));
        assertThrows(InvalidFieldException.class, () -> new TourOperatorEmail("hola@"));
        assertThrows(InvalidFieldException.class, () -> new TourOperatorEmail("a@b@c.test"));
        assertThrows(InvalidFieldException.class, () -> new TourOperatorEmail("hola@acme"));
        assertThrows(InvalidFieldException.class, () -> new TourOperatorEmail("ho la@acme.test"));
    }

    @Test
    void anEmailStopsAtTheColumnWidth() {
        assertThrows(InvalidFieldException.class,
                () -> new TourOperatorEmail("a".repeat(320) + "@acme.test"));
        assertThrows(InvalidFieldException.class, () -> new TourOperatorEmail(""));
        assertThrows(InvalidFieldException.class, () -> new TourOperatorEmail(null));
    }
}
