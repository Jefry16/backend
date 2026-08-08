package com.vointika.touroperator.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/**
 * The shop's public phone number, as the operator writes it.
 *
 * <p><b>No format is imposed, deliberately.</b> V9 left the column without a
 * CHECK because "E.164-vs-national is a product decision nobody has made", and
 * that is still true: this number is rendered as text in a storefront footer,
 * not dialled by us. Forcing E.164 would reject the national formats operators
 * actually print on their own material.
 *
 * <p>What is enforced is what a stored value must satisfy regardless: no control
 * characters (it lands in HTML) and the column's 30.
 */
public record TourOperatorPhone(String value) {

    public static final int MAX_LENGTH = 30;

    public TourOperatorPhone {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Phone cannot be blank");
        }
        for (int i = 0, len = value.length(); i < len; i++) {
            int type = Character.getType(value.charAt(i));
            if (type == Character.CONTROL || type == Character.FORMAT) {
                throw new InvalidFieldException("Phone contains an invalid character");
            }
        }
        value = value.trim();
        if (value.isEmpty() || value.length() > MAX_LENGTH) {
            throw new InvalidFieldException(
                    "Phone must be between 1 and " + MAX_LENGTH + " characters");
        }
    }
}
