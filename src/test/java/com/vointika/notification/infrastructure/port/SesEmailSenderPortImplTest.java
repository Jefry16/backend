package com.vointika.notification.infrastructure.port;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The From-header builder is the header-injection boundary guard — tested as a
 * static (the SES client itself needs live AWS wiring and stays untested here).
 */
class SesEmailSenderPortImplTest {

    private static final String FROM = "noreply@vointika.com";

    @Test
    void nullDisplayNameKeepsTheBareFromAddress() {
        // The platform-branded path must stay byte-identical.
        assertEquals(FROM, SesEmailSenderPortImpl.buildFromHeader(null, FROM));
    }

    @Test
    void displayNameIsQuotedOverTheFromAddress() {
        assertEquals("\"Acme Tours via Vointika\" <" + FROM + ">",
                SesEmailSenderPortImpl.buildFromHeader("Acme Tours via Vointika", FROM));
    }

    @Test
    void crlfAndAngleBracketsAreStripped_neverEncoded() {
        // A folded header is an injection vector — strip, don't escape.
        assertEquals("\"AcmeBcc: victim@example.com evil\" <" + FROM + ">",
                SesEmailSenderPortImpl.buildFromHeader(
                        "Acme\r\nBcc: victim@example.com <evil>", FROM));
    }

    @Test
    void quotesAndBackslashesAreRemoved() {
        assertEquals("\"Acme Tours\" <" + FROM + ">",
                SesEmailSenderPortImpl.buildFromHeader("Acme \"Tours\\\"", FROM));
    }

    @Test
    void blankAfterSanitizingFallsBackToTheBareAddress() {
        assertEquals(FROM, SesEmailSenderPortImpl.buildFromHeader("  <>\r\n ", FROM));
    }

    @Test
    void overlongDisplayNamesAreCapped() {
        String longName = "x".repeat(200);
        String header = SesEmailSenderPortImpl.buildFromHeader(longName, FROM);
        assertTrue(header.length() < 120);
        assertTrue(header.endsWith("<" + FROM + ">"));
    }
}
