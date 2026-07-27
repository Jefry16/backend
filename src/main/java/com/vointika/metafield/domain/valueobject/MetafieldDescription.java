package com.vointika.metafield.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/** Optional help text (≤500). Absence is modelled outside this type. */
public record MetafieldDescription(String value) {

    public MetafieldDescription {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Metafield description cannot be blank");
        }
        for (int i = 0, len = value.length(); i < len; i++) {
            int type = Character.getType(value.charAt(i));
            if (type == Character.CONTROL || type == Character.FORMAT) {
                throw new InvalidFieldException("Metafield description contains an invalid character");
            }
        }
        value = value.trim();
        if (value.isEmpty() || value.length() > 500) {
            throw new InvalidFieldException(
                    "Metafield description must be between 1 and 500 characters");
        }
    }
}
