package com.vointika.touroperator.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/**
 * A store policy's heading — and, unlike every other page type the storefront
 * serves, its {@code <title>} too. A policy has no SEO override because its type
 * is its address, so this one string is both the heading and the tab.
 *
 * <p>200 characters, matching {@code tour_operator_policies.title} and the
 * nullable column that overlays it per locale.
 */
public record PolicyTitle(String value) {

    public static final int MAX_LENGTH = 200;

    public PolicyTitle {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Policy title cannot be blank");
        }
        for (int i = 0, len = value.length(); i < len; i++) {
            int type = Character.getType(value.charAt(i));
            if (type == Character.CONTROL || type == Character.FORMAT) {
                throw new InvalidFieldException("Policy title contains an invalid character");
            }
        }
        value = value.trim();
        if (value.isEmpty() || value.length() > MAX_LENGTH) {
            throw new InvalidFieldException(
                    "Policy title must be between 1 and " + MAX_LENGTH + " characters");
        }
    }
}
