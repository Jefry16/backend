package com.vointika.touroperator.presentation.response;

import com.vointika.touroperator.application.dto.output.AddressView;

import java.util.UUID;

/**
 * The operator's address, read back.
 *
 * <p><b>Null when the operator has no address</b>, rather than an object of
 * nulls — an editor guards on the object, and every operator created before the
 * structured-address slice is in that state until someone re-enters one.
 *
 * <p>The country comes back three ways because each has a caller: the id is what
 * the form posts, the code is what a client localizes from, and the name is what
 * a person reads.
 */
public record AddressResponse(String address1,
                              String address2,
                              String city,
                              String province,
                              String zip,
                              UUID countryId,
                              String countryCode,
                              String countryName) {

    public static AddressResponse from(AddressView view) {
        if (view == null) {
            return null;
        }
        return new AddressResponse(view.address1(), view.address2(), view.city(),
                view.province(), view.zip(), view.countryId(), view.countryCode(),
                view.countryName());
    }
}
