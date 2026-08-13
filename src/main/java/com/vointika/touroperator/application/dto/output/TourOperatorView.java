package com.vointika.touroperator.application.dto.output;

import com.vointika.reference.domain.entity.Country;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;

import java.time.Instant;
import java.util.UUID;

/** The operator's own details, flattened to primitives for the wire. */
public record TourOperatorView(
        UUID id,
        String name,
        String handle,
        AddressView address,
        String phone,
        String email,
        UUID timezoneId,
        UUID currencyId,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * @param country the operator's country, already read; null when the
     *                operator has no address yet — every operator that predates
     *                the structured-address slice is in that state, and this is
     *                the screen they would visit to fix it, so it must not throw
     */
    public static TourOperatorView from(TourOperator o, Country country) {
        return new TourOperatorView(
                o.getId(), o.getName().value(), o.getHandle().value(), address(o.getAddress(), country),
                o.getPhone() == null ? null : o.getPhone().value(),
                o.getEmail() == null ? null : o.getEmail().value(),
                o.getTimezoneId(), o.getCurrencyId(),
                o.getCreatedAt(), o.getUpdatedAt());
    }

    private static AddressView address(TourOperatorAddress address, Country country) {
        if (address == null) {
            return null;
        }
        return new AddressView(
                address.address1(), address.address2(), address.street(),
                address.city(), address.province(), address.zip(),
                address.countryId(),
                country == null ? null : country.getCode(),
                country == null ? null : country.getName());
    }
}
