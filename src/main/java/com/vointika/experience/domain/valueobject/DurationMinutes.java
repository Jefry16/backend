package com.vointika.experience.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/** Experience duration in minutes — required, &gt; 0 and ≤ 14,400 (240h). */
public record DurationMinutes(int value) {

    public DurationMinutes {
        if (value <= 0) {
            throw new InvalidFieldException("Duration must be greater than 0 minutes");
        }
        if (value > 14_400) {
            throw new InvalidFieldException("Duration must be at most 14400 minutes");
        }
    }
}
