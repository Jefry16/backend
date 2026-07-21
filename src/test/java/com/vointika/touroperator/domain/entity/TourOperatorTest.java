package com.vointika.touroperator.domain.entity;

import com.vointika.touroperator.domain.valueobject.Slug;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}
