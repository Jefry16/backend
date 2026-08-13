package com.vointika.touroperator.presentation.request;

import com.vointika.touroperator.application.dto.input.AddressInput;

import java.util.UUID;

/**
 * A postal address on the wire. {@code address1}, {@code city} and
 * {@code countryId} are required; the rest are optional and a blank is stored as
 * an absence.
 *
 * <p>{@code countryId} and not a country code, so it sits beside
 * {@code timezoneId} and {@code currencyId} in the same payload rather than
 * mixing two ways of naming a reference row. {@code GET /api/countries} is where
 * a form gets the ids.
 */
public record AddressRequest(String address1,
                             String address2,
                             String city,
                             String province,
                             String zip,
                             UUID countryId) {

    public AddressInput toInput() {
        return new AddressInput(address1, address2, city, province, zip, countryId);
    }
}
