package com.vointika.touroperator.domain.entity;

import com.vointika.shared.valueobject.Slug;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TourOperatorTest {

    private TourOperator operator() {
        return new TourOperator(
                UUID.randomUUID(),
                new TourOperatorName("Acme Tours"),
                new Slug("acme-tours"),
                UUID.randomUUID(), UUID.randomUUID(),
                new TourOperatorAddress("123 Beach Rd"),
                UUID.randomUUID());
    }

    @Test
    void brandNewOperatorSetsFieldsAndTimestamps() {
        UUID id = UUID.randomUUID();
        UUID tz = UUID.randomUUID();
        UUID cur = UUID.randomUUID();
        UUID owner = UUID.randomUUID();

        TourOperator op = new TourOperator(
                id,
                new TourOperatorName("Acme Tours"),
                new Slug("acme-tours"),
                tz, cur,
                new TourOperatorAddress("123 Beach Rd"),
                owner);

        assertEquals(id, op.getId());
        assertEquals("Acme Tours", op.getName().value());
        assertEquals("acme-tours", op.getSlug().value());
        assertEquals(tz, op.getTimezoneId());
        assertEquals(cur, op.getCurrencyId());
        assertEquals("123 Beach Rd", op.getAddress().value());
        assertEquals(owner, op.getCreatedBy());
        assertNotNull(op.getCreatedAt());
        assertNotNull(op.getUpdatedAt());
        assertNull(op.getLogoMediaId(), "a brand-new operator has no logo");
    }

    @Test
    void setLogoPointsAtTheMediaId() {
        TourOperator op = operator();
        UUID logoId = UUID.randomUUID();

        op.setLogo(logoId);

        assertEquals(logoId, op.getLogoMediaId());
    }

    @Test
    void clearLogoRemovesIt() {
        TourOperator op = operator();
        op.setLogo(UUID.randomUUID());

        op.clearLogo();

        assertNull(op.getLogoMediaId());
    }

    @Test
    void brandNewOperatorDefaultsToEnglish() {
        TourOperator op = operator();
        assertEquals("en", op.getPrimaryLocale().value());
        assertEquals(java.util.Set.of(com.vointika.shared.valueobject.LocaleCode.of("en")),
                op.getSupportedLocales());
    }

    @Test
    void updateLocalesReplacesPrimaryAndSupported() {
        TourOperator op = operator();
        var en = com.vointika.shared.valueobject.LocaleCode.of("en");
        var es = com.vointika.shared.valueobject.LocaleCode.of("es");

        op.updateLocales(es, new java.util.LinkedHashSet<>(java.util.List.of(en, es)));

        assertEquals("es", op.getPrimaryLocale().value());
        assertEquals(java.util.Set.of(en, es), op.getSupportedLocales());
    }

    @Test
    void updateLocalesRejectsEmptySupported() {
        TourOperator op = operator();
        assertThrows(com.vointika.shared.exception.InvalidFieldException.class,
                () -> op.updateLocales(com.vointika.shared.valueobject.LocaleCode.of("en"), java.util.Set.of()));
    }

    @Test
    void updateLocalesRejectsPrimaryNotInSupported() {
        TourOperator op = operator();
        assertThrows(com.vointika.shared.exception.InvalidFieldException.class,
                () -> op.updateLocales(com.vointika.shared.valueobject.LocaleCode.of("de"),
                        java.util.Set.of(com.vointika.shared.valueobject.LocaleCode.of("en"))));
    }
}
