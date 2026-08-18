package com.vointika.experience.application.usecase;

import com.vointika.shared.exception.InvalidFieldException;

import java.time.LocalDate;

/** Shared slot-date bounds (operator-local): today or later, at most 24 months ahead. */
final class SlotScheduling {

    static final int MAX_MONTHS_AHEAD = 24;

    static void requireWithinWindow(LocalDate date, LocalDate today) {
        if (date.isBefore(today)) {
            throw new InvalidFieldException("Date must be today or later");
        }
        if (date.isAfter(today.plusMonths(MAX_MONTHS_AHEAD))) {
            throw new InvalidFieldException(
                    "Date can be at most " + MAX_MONTHS_AHEAD + " months ahead");
        }
    }

    private SlotScheduling() {}
}
