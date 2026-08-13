package com.vointika.touroperator.application.dto.output;

import java.util.UUID;

/**
 * An operator's address, flattened for the wire with its country resolved.
 *
 * <p>The country arrives as id, code and name together because each has a
 * caller: the id is what a form posts back, the code is what a client localizes
 * from, and the name is what a human reads. Resolving it is the use case's job —
 * a view is built from primitives and cannot query.
 */
public record AddressView(String address1,
                          String address2,
                          String street,
                          String city,
                          String province,
                          String zip,
                          UUID countryId,
                          String countryCode,
                          String countryName) {}
