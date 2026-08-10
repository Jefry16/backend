package com.vointika.experience.domain.entity;

import java.math.BigDecimal;
import com.vointika.experience.domain.valueobject.Price;
import com.vointika.experience.domain.valueobject.BookingCutoffHours;
import com.vointika.experience.domain.valueobject.Description;
import com.vointika.experience.domain.valueobject.ExperienceName;
import com.vointika.experience.domain.valueobject.LongDescription;
import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.valueobject.Handle;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperienceTest {

    private Experience create(List<UUID> mediaIds, UUID thumbnail) {
        return Experience.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new Handle("dive-trip"),
                new ExperienceName("Dive Trip"), new Description("A dive"), new LongDescription("Long"),
                false, mediaIds, thumbnail, new BookingCutoffHours(24), null, null, new Price(new BigDecimal("35.00")));
    }

    @Test
    void newExperienceStartsUnpublished() {
        assertFalse(create(List.of(), null).isPublished());
    }

    @Test
    void publishAndUnpublishTransition() {
        Experience e = create(List.of(), null);
        e.publish();
        assertTrue(e.isPublished());
        e.unpublish();
        assertFalse(e.isPublished());
    }

    @Test
    void publishingAPublishedExperienceConflicts() {
        Experience e = create(List.of(), null);
        e.publish();
        assertThrows(ConflictException.class, e::publish);
    }

    @Test
    void unpublishingADraftConflicts() {
        assertThrows(ConflictException.class, () -> create(List.of(), null).unpublish());
    }

    @Test
    void thumbnailMustBeOneOfTheMediaIds() {
        UUID inGallery = UUID.randomUUID();
        // ok: thumbnail is in the gallery
        create(List.of(inGallery), inGallery);
        // reject: thumbnail not in the gallery
        assertThrows(InvalidFieldException.class, () -> create(List.of(inGallery), UUID.randomUUID()));
    }

    @Test
    void mediaCapEnforced() {
        List<UUID> tooMany = IntStream.range(0, 21).mapToObj(i -> UUID.randomUUID()).toList();
        assertThrows(InvalidFieldException.class, () -> create(tooMany, null));
    }


    @Test
    void updateReplacesEditableFieldsAndRevalidates() {
        Experience e = create(List.of(), null);
        UUID m = UUID.randomUUID();
        e.update(new ExperienceName("New"), new Description("d2"), new LongDescription("l2"),
                true, List.of(m), m, new BookingCutoffHours(12), null, null, new Price(new BigDecimal("35.00")));
        assertEquals("New", e.getName().value());
        assertEquals(true, e.isFeatured());
        assertEquals(m, e.getThumbnailMediaId());
    }
}
