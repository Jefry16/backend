package com.vointika.metafield.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/** The definition's display name: 1–120 after trim, control/format characters rejected. */
public record MetafieldDefinitionName(String value) {

    public MetafieldDefinitionName {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Metafield definition name cannot be blank");
        }
        for (int i = 0, len = value.length(); i < len; i++) {
            int type = Character.getType(value.charAt(i));
            if (type == Character.CONTROL || type == Character.FORMAT) {
                throw new InvalidFieldException("Metafield definition name contains an invalid character");
            }
        }
        value = value.trim();
        if (value.isEmpty() || value.length() > 120) {
            throw new InvalidFieldException(
                    "Metafield definition name must be between 1 and 120 characters");
        }
    }
}
