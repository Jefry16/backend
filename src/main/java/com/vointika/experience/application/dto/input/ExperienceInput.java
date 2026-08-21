package com.vointika.experience.application.dto.input;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The editable fields of an experience — shared by create and update, and bound
 * directly by the controller (PATTERNS §4c). Raw primitives; the use case builds
 * the value objects (and thus validates) from these. {@code null} lists are
 * treated as empty.
 *
 * <p>There was an {@code ExperienceRequest} beside this with the same ten fields
 * in the same order. Its only difference was boxing {@code featured} so an absent
 * field could default to false — which {@link #isFeatured()} does here instead, so
 * the copy and its mapper were not a seam (§4c: "an identical copy is not a seam").
 *
 * <p><b>Nothing may annotate this record.</b> The application layer's allowlist is
 * {@code com.vointika..} plus {@code java..}, so a {@code @JsonProperty} or a
 * Jakarta constraint compiles and then fails ArchUnit. If the shapes genuinely
 * diverge, bring a presentation record back rather than annotating this one.
 */
public record ExperienceInput(
        String name,
        String description,
        String longDescription,
        Boolean featured,
        List<UUID> mediaIds,
        UUID thumbnailMediaId,
        Integer bookingCutoffHours,
        String seoTitle,
        String seoDescription,
        BigDecimal startingPrice,
        UUID categoryId) {

    /** Absent and false are the same thing: an experience is not featured unless it says so. */
    public boolean isFeatured() {
        return Boolean.TRUE.equals(featured);
    }
}
