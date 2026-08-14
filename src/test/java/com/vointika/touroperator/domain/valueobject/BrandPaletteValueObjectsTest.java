package com.vointika.touroperator.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class BrandPaletteValueObjectsTest {


    @Test
    void aColourKeepsItsValue() {
        assertEquals("#0b3d5c", new HexColor("#0b3d5c").value());
    }

    @Test
    void aColourIsLowerCasedToMatchTheCheckConstraint() {
        // The column CHECK is ~ '^#[0-9a-f]{6}$', so an operator pasting the hex
        // out of a design tool would otherwise be an untranslated 23514.
        assertEquals("#0b3d5c", new HexColor("#0B3D5C").value());
        assertEquals("#ffffff", new HexColor("  #FFFFFF  ").value());
    }

    @Test
    void aColourRejectsShorthandRatherThanExpandingIt() {
        // A value that round-trips differently from what was sent is worse than
        // a refusal, and the column is VARCHAR(7).
        assertThrows(InvalidFieldException.class, () -> new HexColor("#abc"));
    }

    @Test
    void aColourRejectsWhatIsNotAHexTriplet() {
        assertThrows(InvalidFieldException.class, () -> new HexColor("0b3d5c"));
        assertThrows(InvalidFieldException.class, () -> new HexColor("#0b3d5"));
        assertThrows(InvalidFieldException.class, () -> new HexColor("#0b3d5cc"));
        assertThrows(InvalidFieldException.class, () -> new HexColor("#gggggg"));
        assertThrows(InvalidFieldException.class, () -> new HexColor("rebeccapurple"));
        assertThrows(InvalidFieldException.class, () -> new HexColor(""));
        assertThrows(InvalidFieldException.class, () -> new HexColor(null));
    }


    @Test
    void aSocialUrlKeepsItsValue() {
        assertEquals("https://instagram.com/acme",
                new SocialUrl("  https://instagram.com/acme  ").value());
        assertDoesNotThrow(() -> new SocialUrl("http://example.com"));
    }

    @Test
    void aSocialUrlRefusesEverySchemeButHttpAndHttps() {
        // This value is rendered straight into an href on a public page, and
        // Mustache's escaper is a seven-pair replace, not a URL sanitiser. The
        // menus context carries this hole as recorded debt; a new write path
        // should not add a second.
        assertThrows(InvalidFieldException.class,
                () -> new SocialUrl("javascript:alert(document.cookie)"));
        assertThrows(InvalidFieldException.class, () -> new SocialUrl("data:text/html,<script>"));
        assertThrows(InvalidFieldException.class, () -> new SocialUrl("ftp://example.com"));
        assertThrows(InvalidFieldException.class, () -> new SocialUrl("//example.com"));
        assertThrows(InvalidFieldException.class, () -> new SocialUrl("instagram.com/acme"));
    }

    @Test
    void aSocialUrlIsCaseInsensitiveAboutItsScheme() {
        // "JavaScript:" is the obvious way past a case-sensitive check.
        assertThrows(InvalidFieldException.class, () -> new SocialUrl("JavaScript:alert(1)"));
        assertDoesNotThrow(() -> new SocialUrl("HTTPS://instagram.com/acme"));
    }

    @Test
    void aSocialUrlNeedsAHost() {
        assertThrows(InvalidFieldException.class, () -> new SocialUrl("https://"));
        assertThrows(InvalidFieldException.class, () -> new SocialUrl("https:///path"));
    }

    @Test
    void aSocialUrlStopsAtTheColumnWidth() {
        String long_ = "https://example.com/" + "a".repeat(SocialUrl.MAX_LENGTH);
        assertThrows(InvalidFieldException.class, () -> new SocialUrl(long_));
        assertThrows(InvalidFieldException.class, () -> new SocialUrl(""));
        assertThrows(InvalidFieldException.class, () -> new SocialUrl(null));
    }
}
