package com.vointika.audience.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/**
 * Number of people covered by one unit of an audience tier.
 * 1 for per-person tiers (Adults, Seniors); &gt;1 for group/package tiers
 * ("VIP Table for 6" → paxPerUnit=6). Always positive.
 */
public record PaxPerUnit(int value) {

    public PaxPerUnit {
        if (value <= 0) {
            throw new InvalidFieldException("Pax per unit must be positive");
        }
    }

    /** HTTP-boundary null-safe form — the body may omit the field, default to 1. */
    public PaxPerUnit(Integer value) {
        this(value == null ? 1 : value);
    }
}
