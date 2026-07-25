package com.vointika.experience.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/** A slot's per-audience seat count. 1–100000. */
public record Capacity(int value) {

    public Capacity {
        if (value <= 0) {
            throw new InvalidFieldException("Capacity must be positive");
        }
        if (value > 100_000) {
            throw new InvalidFieldException("Capacity must be at most 100000");
        }
    }

    public Capacity(Integer value) {
        this(requireNonNull(value));
    }

    private static int requireNonNull(Integer value) {
        if (value == null) {
            throw new InvalidFieldException("Capacity is required");
        }
        return value;
    }
}
