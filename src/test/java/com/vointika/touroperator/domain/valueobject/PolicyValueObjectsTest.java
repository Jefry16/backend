package com.vointika.touroperator.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PolicyValueObjectsTest {


    @Test
    void aTitleKeepsItsTextAndIsTrimmed() {
        assertEquals("Cancellation policy", new PolicyTitle("  Cancellation policy  ").value());
    }

    @Test
    void aBlankTitleIsRejected() {
        assertThrows(InvalidFieldException.class, () -> new PolicyTitle(""));
        assertThrows(InvalidFieldException.class, () -> new PolicyTitle("   "));
        assertThrows(InvalidFieldException.class, () -> new PolicyTitle(null));
    }

    @Test
    void aTitleStopsAtTheColumnWidth() {
        assertEquals(200, new PolicyTitle("a".repeat(200)).value().length());
        assertThrows(InvalidFieldException.class, () -> new PolicyTitle("a".repeat(201)));
    }

    @Test
    void aTitleRejectsControlCharacters() {
        assertThrows(InvalidFieldException.class, () -> new PolicyTitle("Can" + (char) 0 + "cel"));
        assertThrows(InvalidFieldException.class, () -> new PolicyTitle("Two\nlines"));
    }


    @Test
    void aBodyKeepsItsHtmlExactly() {
        // Verbatim: no trim, no escape, no sanitising. The storefront renders this
        // unescaped on purpose, so anything this constructor did to it would ship.
        String html = "<h2>Cancellations</h2>\n<p>Free up to <strong>48h</strong> before.</p>";
        assertEquals(html, new PolicyBody(html).value());
    }

    @Test
    void aBodyKeepsMarkupThatLooksDangerous() {
        // Deliberate, and the reason there is no sanitiser here: this is the
        // operator's own document on their own storefront, the boundary Shopify's
        // policy pages sit on. Reading this as a defect leads to "fixing" it into
        // &lt;script&gt; on every visitor's screen.
        String html = "<p>See <a href=\"/terms\">terms</a></p><script>track()</script>";
        assertEquals(html, new PolicyBody(html).value());
    }

    @Test
    void aBlankBodyIsRejected() {
        assertThrows(InvalidFieldException.class, () -> new PolicyBody(""));
        assertThrows(InvalidFieldException.class, () -> new PolicyBody("   "));
        assertThrows(InvalidFieldException.class, () -> new PolicyBody(null));
    }

    @Test
    void aBodyStopsAtTheCap() {
        assertDoesNotThrow(() -> new PolicyBody("a".repeat(PolicyBody.MAX_LENGTH)));
        assertThrows(InvalidFieldException.class,
                () -> new PolicyBody("a".repeat(PolicyBody.MAX_LENGTH + 1)));
    }

    @Test
    void aBodyRejectsNulBecausePostgresTextCannotHoldOne() {
        assertThrows(InvalidFieldException.class, () -> new PolicyBody("ab" + (char) 0 + "cd"));
    }

    @Test
    void aBodyAllowsNewlinesUnlikeATitle() {
        // A document is multi-line; a heading is not. The two VOs differ here on
        // purpose, so this pins the difference rather than leaving it to reading.
        assertDoesNotThrow(() -> new PolicyBody("<p>one</p>\n<p>two</p>"));
        assertThrows(InvalidFieldException.class, () -> new PolicyTitle("one\ntwo"));
    }
}
