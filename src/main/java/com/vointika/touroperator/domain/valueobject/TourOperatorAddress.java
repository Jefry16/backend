package com.vointika.touroperator.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

import java.util.UUID;

/**
 * The operator's postal address, in parts.
 *
 * <p>It was one free-text line until the storefront needed to publish it: a
 * theme wants the city and the country separately, most concretely for
 * schema.org {@code LocalBusiness} markup, which is how a tour operator surfaces
 * in local search. The old value could not be parsed into these parts — that is
 * why the column was dropped rather than migrated, and why an operator who has
 * not re-entered theirs has <b>no</b> address rather than a half-filled one.
 *
 * <p><b>One record rather than six value objects.</b> "A city is a non-blank
 * string of at most 120 characters" is not a rule that earns a type; the small
 * value objects in this project exist where the rule is real and reused
 * ({@code Email}, {@code HexColor}, {@code SocialUrl}). {@code zip} is the only
 * part with a plausible rule of its own, and there is no universal postcode
 * format — validating it per country needs a table we do not have, the same call
 * V9 made for phone.
 *
 * <p><b>Only the country is a reference.</b> ISO 3166-1 is a closed set of 249
 * that changes about once a decade, so a foreign key blocks nobody. Cities are
 * millions with no canonical list, and {@code city} is required — a curated
 * table would stop an operator whose village is missing from finishing signup,
 * which is the failure a reference table exists to prevent, inverted. Shopify
 * draws the line in the same place.
 *
 * @param address1  required — the street line
 * @param address2  optional second line; blank is stored as null, because a
 *                  blank is not an absence and a renderer treats {@code ""} as
 *                  present
 * @param countryId required, and validated against {@code reference.country} by
 *                  the use case — a value object cannot query
 */
public record TourOperatorAddress(String address1,
                                  String address2,
                                  String city,
                                  String province,
                                  String zip,
                                  UUID countryId) {

    public static final int LINE_MAX_LENGTH = 255;
    public static final int CITY_MAX_LENGTH = 120;
    public static final int PROVINCE_MAX_LENGTH = 120;
    public static final int ZIP_MAX_LENGTH = 20;

    public TourOperatorAddress {
        address1 = required(address1, "Address line 1", LINE_MAX_LENGTH);
        city = required(city, "City", CITY_MAX_LENGTH);
        address2 = optional(address2, "Address line 2", LINE_MAX_LENGTH);
        province = optional(province, "Province", PROVINCE_MAX_LENGTH);
        zip = optional(zip, "Postcode", ZIP_MAX_LENGTH);
        if (countryId == null) {
            throw new InvalidFieldException("Country is required");
        }
    }

    /**
     * Shopify's derived {@code street} — the two lines as one. Joined with a
     * comma rather than their space, because "Calle Mayor 1 Piso 3" reads as one
     * malformed line and "Calle Mayor 1, Piso 3" reads as two.
     */
    public String street() {
        return address2 == null ? address1 : address1 + ", " + address2;
    }

    private static String required(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException(label + " is required");
        }
        return checked(value, label, maxLength);
    }

    private static String optional(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return checked(value, label, maxLength);
    }

    private static String checked(String value, String label, int maxLength) {
        if (value.indexOf('\0') >= 0) {
            throw new InvalidFieldException(label + " contains an invalid character");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new InvalidFieldException(label + " must be at most " + maxLength + " characters");
        }
        return trimmed;
    }
}
