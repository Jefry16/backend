package com.vointika.touroperator.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/**
 * The brand's one-line tagline — Shopify's {@code shop.brand.slogan}, and the
 * shortest piece of copy a theme renders under the logo.
 *
 * <p>80 characters, matching {@code tour_operator_brand.slogan} and its column
 * on the translation overlay. The width is the line itself: a slogan that wraps
 * is not a slogan, and the canonical row and the overlay have to agree or a
 * translation could be stored that its canonical column could not hold.
 *
 * <p>Absence is modelled outside this type — an operator holds no slogan rather
 * than a blank one.
 */
public record BrandSlogan(String value) {

    public BrandSlogan {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Slogan cannot be blank");
        }
        for (int i = 0, len = value.length(); i < len; i++) {
            int type = Character.getType(value.charAt(i));
            if (type == Character.CONTROL || type == Character.FORMAT) {
                throw new InvalidFieldException("Slogan contains an invalid character");
            }
        }
        value = value.trim();
        if (value.isEmpty() || value.length() > 80) {
            throw new InvalidFieldException("Slogan must be between 1 and 80 characters");
        }
    }
}
