package com.vointika.experience.presentation.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The create/update body for an experience. {@code featured} defaults to false;
 * null lists are treated as empty. Handle and status are not settable here (handle
 * is generated + immutable; status changes via publish/unpublish).
 */
public record ExperienceRequest(
        String name,
        String description,
        String longDescription,
        Boolean featured,
        List<UUID> mediaIds,
        UUID thumbnailMediaId,
        Integer durationMinutes,
        Integer bookingCutoffHours,
        String seoTitle,
        String seoDescription,
        BigDecimal startingPrice) {
}
