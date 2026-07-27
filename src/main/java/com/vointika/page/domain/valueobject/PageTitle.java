package com.vointika.page.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/** The page's display title: 1–255 after trim, control/format characters rejected. */
public record PageTitle(String value) {

    public PageTitle {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Page title cannot be blank");
        }
        for (int i = 0, len = value.length(); i < len; i++) {
            int type = Character.getType(value.charAt(i));
            if (type == Character.CONTROL || type == Character.FORMAT) {
                throw new InvalidFieldException("Page title contains an invalid character");
            }
        }
        value = value.trim();
        if (value.isEmpty() || value.length() > 255) {
            throw new InvalidFieldException("Page title must be between 1 and 255 characters");
        }
    }
}
