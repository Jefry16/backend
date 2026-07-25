package com.vointika.pickup.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

public record PickupLocationName(String value) {

    public PickupLocationName {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Pickup location name cannot be blank");
        }
        for (int i = 0, len = value.length(); i < len; i++) {
            int type = Character.getType(value.charAt(i));
            if (type == Character.CONTROL || type == Character.FORMAT) {
                throw new InvalidFieldException("Pickup location name contains an invalid character");
            }
        }
        value = value.trim();
        if (value.isEmpty() || value.length() > 200) {
            throw new InvalidFieldException("Pickup location name must be between 1 and 200 characters");
        }
    }
}
