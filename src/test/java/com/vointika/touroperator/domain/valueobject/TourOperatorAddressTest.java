package com.vointika.touroperator.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * The address is published on a public page, so what it refuses matters as much
 * as what it stores.
 */
class TourOperatorAddressTest {

    private static final UUID SPAIN = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static TourOperatorAddress address(String address1, String address2, String city,
                                               String province, String zip, UUID countryId) {
        return new TourOperatorAddress(address1, address2, city, province, zip, countryId);
    }

    @Test
    void aCompleteAddressKeepsEveryPart() {
        TourOperatorAddress a = address("Calle Mayor 1", "Piso 3", "Madrid", "Madrid", "28013", SPAIN);

        assertEquals("Calle Mayor 1", a.address1());
        assertEquals("Piso 3", a.address2());
        assertEquals("Madrid", a.city());
        assertEquals("Madrid", a.province());
        assertEquals("28013", a.zip());
        assertEquals(SPAIN, a.countryId());
    }

    /** Only the parts an address cannot do without. */
    @Test
    void streetCityAndCountryAreRequired() {
        assertThrows(InvalidFieldException.class,
                () -> address(null, null, "Madrid", null, null, SPAIN));
        assertThrows(InvalidFieldException.class,
                () -> address("  ", null, "Madrid", null, null, SPAIN));
        assertThrows(InvalidFieldException.class,
                () -> address("Calle Mayor 1", null, null, null, null, SPAIN));
        assertThrows(InvalidFieldException.class,
                () -> address("Calle Mayor 1", null, "Madrid", null, null, null));
    }

    @Test
    void theOptionalPartsMayBeAbsent() {
        assertDoesNotThrow(() -> address("Calle Mayor 1", null, "Madrid", null, null, SPAIN));
    }

    /**
     * A blank is not an absence. Storing {@code ""} would make a renderer treat
     * an empty second line as present and emit an empty element — the reason V9
     * gave for phone and email.
     */
    @Test
    void aBlankOptionalPartIsStoredAsNull() {
        TourOperatorAddress a = address("Calle Mayor 1", "   ", "Madrid", "", " ", SPAIN);

        assertNull(a.address2());
        assertNull(a.province());
        assertNull(a.zip());
    }

    @Test
    void everyPartIsTrimmed() {
        TourOperatorAddress a = address("  Calle Mayor 1 ", " Piso 3 ", "  Madrid  ",
                " Madrid ", " 28013 ", SPAIN);

        assertEquals("Calle Mayor 1", a.address1());
        assertEquals("Piso 3", a.address2());
        assertEquals("Madrid", a.city());
        assertEquals("28013", a.zip());
    }

    @Test
    void eachPartHasItsOwnLengthLimit() {
        assertThrows(InvalidFieldException.class, () -> address(
                "x".repeat(TourOperatorAddress.LINE_MAX_LENGTH + 1), null, "Madrid", null, null, SPAIN));
        assertThrows(InvalidFieldException.class, () -> address(
                "Calle Mayor 1", null, "x".repeat(TourOperatorAddress.CITY_MAX_LENGTH + 1), null, null, SPAIN));
        assertThrows(InvalidFieldException.class, () -> address(
                "Calle Mayor 1", null, "Madrid", null, "x".repeat(TourOperatorAddress.ZIP_MAX_LENGTH + 1), SPAIN));
        assertDoesNotThrow(() -> address(
                "x".repeat(TourOperatorAddress.LINE_MAX_LENGTH), null,
                "y".repeat(TourOperatorAddress.CITY_MAX_LENGTH), null, null, SPAIN));
    }

    /** Carried over from the single-line value object: a NUL byte is never content. */
    @Test
    void aNulCharacterIsRefusedInEveryPart() {
        assertThrows(InvalidFieldException.class,
                () -> address("Calle\0Mayor", null, "Madrid", null, null, SPAIN));
        assertThrows(InvalidFieldException.class,
                () -> address("Calle Mayor 1", null, "Mad\0rid", null, null, SPAIN));
        assertThrows(InvalidFieldException.class,
                () -> address("Calle Mayor 1", "Piso\0 3", "Madrid", null, null, SPAIN));
    }

    /** Shopify's derived {@code street}: the two lines as one, or just the one. */
    @Test
    void streetJoinsTheTwoLines() {
        assertEquals("Calle Mayor 1, Piso 3",
                address("Calle Mayor 1", "Piso 3", "Madrid", null, null, SPAIN).street());
        assertEquals("Calle Mayor 1",
                address("Calle Mayor 1", null, "Madrid", null, null, SPAIN).street());
    }
}
