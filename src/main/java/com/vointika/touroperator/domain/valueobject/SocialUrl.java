package com.vointika.touroperator.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

import java.net.URI;
import java.util.Locale;

/**
 * The address of one of the brand's social profiles.
 *
 * <p><b>Restricted to http and https on purpose.</b> This value is rendered
 * straight into an {@code href} on a public page, so accepting an arbitrary
 * scheme accepts {@code javascript:} — and Mustache's HTML escaper does not make
 * an href safe (MAP decision 6, mechanics §5: it is a seven-pair replace, not a
 * URL sanitiser). The menus context stores {@code EXTERNAL_URL} verbatim and
 * carries that hole as recorded debt; a new write path should not add a second.
 *
 * <p>500 characters, matching {@code tour_operator_brand_social_links.url}.
 */
public record SocialUrl(String value) {

    public static final int MAX_LENGTH = 500;

    public SocialUrl {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Social link URL cannot be blank");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new InvalidFieldException(
                    "Social link URL must be at most " + MAX_LENGTH + " characters");
        }
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidFieldException("Social link URL is not a valid URL");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new InvalidFieldException("Social link URL must start with http:// or https://");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new InvalidFieldException("Social link URL must include a host");
        }
    }
}
