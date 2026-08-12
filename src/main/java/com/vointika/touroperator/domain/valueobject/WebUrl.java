package com.vointika.touroperator.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

import java.net.URI;
import java.util.Locale;

/**
 * The one rule for any operator-supplied URL this project renders into an
 * {@code href} on a public page: <b>http or https, with a host</b>.
 *
 * <p>It lives in one place because the failure it prevents is the same wherever
 * it appears — an arbitrary scheme accepts {@code javascript:}, and an HTML
 * escaper does not make an href safe (MAP decision 6, mechanics §5: it is a
 * seven-pair replace, not a URL sanitiser). {@link SocialUrl} learned that when
 * the brand shipped; the menus context stored {@code EXTERNAL_URL} verbatim and
 * carried the same hole until it got a second caller here.
 *
 * <p>It is a rule rather than a value object because the two callers differ in
 * the things a value object would fix: their length limits (500 against 2048)
 * and the words in their messages. What must not differ is the scheme check.
 */
public final class WebUrl {

    private WebUrl() {
    }

    /**
     * @param label how this URL is described to whoever typed it — "Social link
     *              URL", "Menu item url"
     */
    public static void requireHttpOrHttps(String value, String label) {
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidFieldException(label + " is not a valid URL");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new InvalidFieldException(label + " must start with http:// or https://");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new InvalidFieldException(label + " must include a host");
        }
    }
}
