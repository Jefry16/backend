package com.vointika.pickup.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

import java.time.DateTimeException;
import java.time.LocalTime;

/** The time-of-day a shopper meets at this point (operator-local, e.g. 09:30). */
public record PickupLocationTime(LocalTime value) {

    public PickupLocationTime {
        if (value == null) {
            throw new InvalidFieldException("Pickup location time cannot be null");
        }
    }

    /** Parse the HTTP-boundary string representation (HH:mm or HH:mm:ss). */
    public PickupLocationTime(String raw) {
        this(parse(raw));
    }

    private static LocalTime parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidFieldException("Pickup location time is required");
        }
        try {
            return LocalTime.parse(raw);
        } catch (DateTimeException e) {
            throw new InvalidFieldException("Pickup location time must be a valid local time (HH:mm)");
        }
    }
}
