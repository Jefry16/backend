package com.vointika.touroperator.domain.entity;

import com.vointika.shared.valueobject.Handle;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TourOperatorTest {

    private TourOperator operator() {
        return new TourOperator(
                UUID.randomUUID(),
                new TourOperatorName("Acme Tours"),
                new Handle("acme-tours"),
                UUID.randomUUID(), UUID.randomUUID(),
                new TourOperatorAddress("123 Beach Rd", null, "Palma", null, null, UUID.randomUUID()),
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
                new Handle("acme-tours"),
                tz, cur,
                new TourOperatorAddress("123 Beach Rd", null, "Palma", null, null, UUID.randomUUID()),
                owner);

        assertEquals(id, op.getId());
        assertEquals("Acme Tours", op.getName().value());
        assertEquals("acme-tours", op.getHandle().value());
        assertEquals(tz, op.getTimezoneId());
        assertEquals(cur, op.getCurrencyId());
        assertEquals("123 Beach Rd", op.getAddress().address1());
        assertEquals(owner, op.getCreatedBy());
        assertNotNull(op.getCreatedAt());
        assertNotNull(op.getUpdatedAt());
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

    /**
     * <b>Six flat keys, not one.</b> {@code AuditChanges.diff} is a flat map diff,
     * so this is what makes the activity log say "zip: 28013 -> 28014" instead of
     * "the address changed". {@code countryId} is a UUID string, exactly as
     * {@code timezoneId} and {@code currencyId} already are.
     */
    @Test
    void theAuditSnapshotFlattensTheAddress() {
        UUID country = UUID.randomUUID();
        TourOperator op = new TourOperator(UUID.randomUUID(), new TourOperatorName("Acme Tours"),
                new Handle("acme"), UUID.randomUUID(), UUID.randomUUID(),
                new TourOperatorAddress("Calle Mayor 1", null, "Madrid", "Madrid", "28013", country),
                UUID.randomUUID());

        Map<String, Object> snapshot = op.auditSnapshot();

        assertTrue(snapshot.keySet().containsAll(
                java.util.List.of("address1", "address2", "city", "province", "zip", "countryId")));
        assertEquals("Calle Mayor 1", snapshot.get("address1"));
        assertEquals("Madrid", snapshot.get("city"));
        assertEquals("28013", snapshot.get("zip"));
        assertEquals(country.toString(), snapshot.get("countryId"));
        assertNull(snapshot.get("address2"));
        assertFalse(snapshot.containsKey("address"));
    }
}
