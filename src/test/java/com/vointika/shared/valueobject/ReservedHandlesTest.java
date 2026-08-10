package com.vointika.shared.valueobject;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A storefront lives at {@code {handle}.vointika.com}, so an operator handle is
 * a hostname and cannot be one infrastructure already answers on.
 */
class ReservedHandlesTest {

    @Test
    void theHostsThisProjectDocumentsAreReserved() {
        assertThat(ReservedHandles.isReserved("api")).isTrue();
        assertThat(ReservedHandles.isReserved("admin")).isTrue();
        assertThat(ReservedHandles.isReserved("media")).isTrue();
        assertThat(ReservedHandles.isReserved("themes")).isTrue();
        assertThat(ReservedHandles.isReserved("staging")).isTrue();
    }

    /**
     * Over-reserving costs an operator their own name, so the match is exact
     * rather than a prefix: a diving school called Apidae keeps its handle.
     */
    @Test
    void anOrdinaryOperatorNameIsNot() {
        assertThat(ReservedHandles.isReserved("acme")).isFalse();
        assertThat(ReservedHandles.isReserved("acme-tours")).isFalse();
        assertThat(ReservedHandles.isReserved("apidae")).isFalse();
        assertThat(ReservedHandles.isReserved("administratie")).isFalse();
        assertThat(ReservedHandles.isReserved("mediterraneo")).isFalse();
    }

    @Test
    void nullIsNotReservedRatherThanAnError() {
        assertThat(ReservedHandles.isReserved(null)).isFalse();
    }

    /** The edge is handed whatever the Host header carried, not a normalized handle. */
    @Test
    void theCheckFoldsCase() {
        assertThat(ReservedHandles.isReserved("API")).isTrue();
        assertThat(ReservedHandles.isReserved("Admin")).isTrue();
    }

    /**
     * PATTERNS §11. Under a Turkish default locale {@code "API".toLowerCase()}
     * is {@code "apı"} — dotless ı — and the label walks straight past the set.
     * Pinned by actually running under that locale rather than trusting that the
     * implementation passes {@code Locale.ROOT}.
     */
    @Test
    void theCaseFoldIsLocaleIndependent() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            assertThat(ReservedHandles.isReserved("API")).isTrue();
            assertThat(ReservedHandles.isReserved("ADMIN")).isTrue();
        } finally {
            Locale.setDefault(original);
        }
    }
}
