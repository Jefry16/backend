package com.vointika.touroperator.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/**
 * The brand's short description — Shopify's {@code shop.brand.short_description},
 * a sentence or two a theme renders where the shop introduces itself.
 *
 * <p>150 characters, matching {@code tour_operator_brand.short_description} and
 * its column on the translation overlay; the canonical row and the overlay have
 * to agree or a translation could be stored that its canonical column could not
 * hold. Distinct from the operator's SEO description (320, SERP-shaped) — this
 * one is body copy, not a meta tag, which is why it is shorter and why both
 * exist.
 *
 * <p>Absence is modelled outside this type — an operator holds no short
 * description rather than a blank one.
 */
public record BrandShortDescription(String value) {

    public BrandShortDescription {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Short description cannot be blank");
        }
        for (int i = 0, len = value.length(); i < len; i++) {
            int type = Character.getType(value.charAt(i));
            if (type == Character.CONTROL || type == Character.FORMAT) {
                throw new InvalidFieldException("Short description contains an invalid character");
            }
        }
        value = value.trim();
        if (value.isEmpty() || value.length() > 150) {
            throw new InvalidFieldException(
                    "Short description must be between 1 and 150 characters");
        }
    }
}
