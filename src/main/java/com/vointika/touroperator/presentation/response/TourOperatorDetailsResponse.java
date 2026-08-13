package com.vointika.touroperator.presentation.response;

import com.vointika.touroperator.application.dto.output.TourOperatorView;

import java.time.Instant;
import java.util.UUID;

/**
 * The operator's own details. A resource, so it carries {@code id} +
 * {@code context} (PATTERNS §4a) — {@code context} set by the second constructor
 * so no caller passes it.
 */
public record TourOperatorDetailsResponse(
        UUID id,
        String context,
        String name,
        String handle,
        AddressResponse address,
        String phone,
        String email,
        UUID timezoneId,
        UUID currencyId,
        Instant createdAt,
        Instant updatedAt) {

    public TourOperatorDetailsResponse(UUID id, String name, String handle, AddressResponse address,
                                       String phone, String email, UUID timezoneId,
                                       UUID currencyId, Instant createdAt, Instant updatedAt) {
        this(id, "tour-operators", name, handle, address, phone, email,
                timezoneId, currencyId, createdAt, updatedAt);
    }

    public static TourOperatorDetailsResponse from(TourOperatorView v) {
        return new TourOperatorDetailsResponse(v.id(), v.name(), v.handle(),
                AddressResponse.from(v.address()),
                v.phone(), v.email(), v.timezoneId(), v.currencyId(), v.createdAt(), v.updatedAt());
    }
}
