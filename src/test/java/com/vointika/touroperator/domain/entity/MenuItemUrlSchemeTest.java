package com.vointika.touroperator.domain.entity;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.touroperator.domain.enums.MenuItemLinkType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A menu item's {@code EXTERNAL_URL} is rendered into an {@code href} on a
 * public page, so an arbitrary scheme is an arbitrary script.
 *
 * <p>This is the same rule {@code SocialUrl} carries and the same test its
 * palette suite runs — deliberately, because the two write paths are what a
 * reader has to trust are in step. The check itself lives once, in
 * {@code WebUrl}.
 *
 * <p>The database says it too, since a domain guard only binds the write paths
 * that go through it: {@code menu_items_url_scheme_check} (touroperator/V14).
 */
class MenuItemUrlSchemeTest {

    private static final UUID MENU = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");

    private static MenuItem externalLink(String url) {
        return MenuItem.create(UUID.randomUUID(), MENU, null, 0, "Blog",
                MenuItemLinkType.EXTERNAL_URL, null, url, 0, Instant.now());
    }

    @Test
    void anExternalLinkTakesHttpAndHttps() {
        assertDoesNotThrow(() -> externalLink("https://example.com/blog"));
        assertDoesNotThrow(() -> externalLink("http://example.com"));
    }

    /**
     * The one that matters. {@code javascript:} in an href runs on click, and no
     * HTML escaper prevents it — escaping makes the <em>text</em> safe, not the
     * scheme.
     */
    @Test
    void anExternalLinkRefusesEverySchemeButHttpAndHttps() {
        assertThrows(InvalidFieldException.class,
                () -> externalLink("javascript:alert(document.cookie)"));
        assertThrows(InvalidFieldException.class, () -> externalLink("data:text/html,<script>"));
        assertThrows(InvalidFieldException.class, () -> externalLink("ftp://example.com"));
        assertThrows(InvalidFieldException.class, () -> externalLink("mailto:hola@acme.test"));
    }

    /** Scheme-relative and path-only values have no scheme to check, so they are refused too. */
    @Test
    void anExternalLinkNeedsASchemeAndAHost() {
        assertThrows(InvalidFieldException.class, () -> externalLink("//example.com"));
        assertThrows(InvalidFieldException.class, () -> externalLink("/experiences"));
        assertThrows(InvalidFieldException.class, () -> externalLink("https://"));
    }

    /** Case is not a bypass. */
    @Test
    void theSchemeCheckFoldsCase() {
        assertDoesNotThrow(() -> externalLink("HTTPS://example.com"));
        assertThrows(InvalidFieldException.class, () -> externalLink("JavaScript:alert(1)"));
    }

    /** The other link types carry no url at all, so there is nothing to validate. */
    @Test
    void aResourceLinkIsUnaffected() {
        assertDoesNotThrow(() -> MenuItem.create(UUID.randomUUID(), MENU, null, 0, "Home",
                MenuItemLinkType.HOME, null, null, 0, Instant.now()));
    }
}
