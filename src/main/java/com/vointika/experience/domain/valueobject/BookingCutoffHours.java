package com.vointika.experience.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/** Advance-notice hours before start that booking closes — ≥ 0 and ≤ 8,760 (1 year). */
public record BookingCutoffHours(int value) {

    public BookingCutoffHours {
        if (value < 0) {
            throw new InvalidFieldException("Booking cutoff hours cannot be negative");
        }
        if (value > 8_760) {
            throw new InvalidFieldException("Booking cutoff hours must be at most 8760");
        }
    }
}
