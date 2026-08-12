package com.vointika.touroperator.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;


/**
 * The address of one of the brand's social profiles.
 *
 * <p><b>Restricted to http and https on purpose.</b> This value is rendered
 * straight into an {@code href} on a public page, so accepting an arbitrary
 * scheme accepts {@code javascript:} — and an HTML escaper does not make an href
 * safe (MAP decision 6, mechanics §5: it is a seven-pair replace, not a URL
 * sanitiser). The check itself moved to {@link WebUrl} when a menu item's
 * {@code EXTERNAL_URL} became its second caller — one rule, two names for it.
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
        WebUrl.requireHttpOrHttps(value, "Social link URL");
    }
}
