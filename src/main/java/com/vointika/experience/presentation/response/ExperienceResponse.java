package com.vointika.experience.presentation.response;

import com.vointika.experience.application.dto.output.ExperienceView;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * An experience for read APIs. {@code id} + {@code context:"experiences"} per the
 * house convention. Media is given both ways: raw references
 * ({@code thumbnailMediaId} + ordered {@code mediaIds}) so an editor can
 * round-trip the selection, and resolved {@code thumbnailUrl} + ordered
 * {@code galleryUrls} (URLs, never stored) for display.
 */
public record ExperienceResponse(
        UUID id,
        String context,
        String name,
        String handle,
        String description,
        String longDescription,
        boolean featured,
        List<String> included,
        List<String> notIncluded,
        UUID thumbnailMediaId,
        String thumbnailUrl,
        List<UUID> mediaIds,
        List<String> galleryUrls,
        int durationMinutes,
        int bookingCutoffHours,
        boolean published,
        BigDecimal startingPrice,
        UUID createdBy,
        Instant createdAt) {

    public static ExperienceResponse from(ExperienceView v) {
        return new ExperienceResponse(
                v.id(), "experiences", v.name(), v.handle(), v.description(), v.longDescription(),
                v.featured(), v.included(), v.notIncluded(),
                v.thumbnailMediaId(), v.thumbnailUrl(), v.mediaIds(), v.galleryUrls(),
                v.durationMinutes(), v.bookingCutoffHours(),
                v.published(), v.startingPrice(), v.createdBy(), v.createdAt());
    }
}
