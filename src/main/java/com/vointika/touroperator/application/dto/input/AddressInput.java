package com.vointika.touroperator.application.dto.input;

import java.util.UUID;

/**
 * A postal address as it arrives on the wire.
 *
 * <p><b>It is patched as a whole, never merged.</b> Sending
 * {@code {"address": {"city": "Barcelona"}}} against a Madrid address would
 * otherwise produce a new city on an old street — a wrong address rather than a
 * partial one, and one that looks entirely plausible. So supplying the object
 * replaces it and its optional parts, and omitting the object leaves it alone.
 * The nesting is what makes that expressible: a record cannot tell an absent
 * field from an explicit null, but it can tell an absent <em>object</em>.
 */
public record AddressInput(String address1,
                           String address2,
                           String city,
                           String province,
                           String zip,
                           UUID countryId) {}
