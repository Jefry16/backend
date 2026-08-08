package com.vointika.touroperator.application.dto.output;

import com.vointika.touroperator.domain.entity.TourOperator;

import java.time.Instant;
import java.util.UUID;

/** The operator's own details, flattened to primitives for the wire. */
public record TourOperatorView(
        UUID id,
        String name,
        String handle,
        String address,
        String phone,
        String email,
        UUID timezoneId,
        UUID currencyId,
        Instant createdAt,
        Instant updatedAt) {

    public static TourOperatorView from(TourOperator o) {
        return new TourOperatorView(
                o.getId(), o.getName().value(), o.getHandle().value(), o.getAddress().value(),
                o.getPhone() == null ? null : o.getPhone().value(),
                o.getEmail() == null ? null : o.getEmail().value(),
                o.getTimezoneId(), o.getCurrencyId(),
                o.getCreatedAt(), o.getUpdatedAt());
    }
}
