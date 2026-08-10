package com.vointika.experience.application.dto.input;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The editable fields of an experience — shared by create and update. Raw
 * primitives; the use case builds the value objects (and thus validates) from
 * these. {@code null} lists are treated as empty.
 */
public record ExperienceInput(
        String name,
        String description,
        String longDescription,
        boolean featured,
        List<String> tags,
        List<String> included,
        List<String> notIncluded,
        List<String> highlights,
        List<UUID> mediaIds,
        UUID thumbnailMediaId,
        Integer durationMinutes,
        Integer bookingCutoffHours,
        String seoTitle,
        String seoDescription,
        BigDecimal startingPrice) {
}
