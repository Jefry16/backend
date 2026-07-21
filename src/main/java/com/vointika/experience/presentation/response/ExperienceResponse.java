package com.vointika.experience.presentation.response;

import com.vointika.experience.application.dto.output.ExperienceView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * An experience for read APIs. {@code id} + {@code context:"experiences"} per the
 * house convention. Media is resolved: {@code thumbnailUrl} + ordered
 * {@code galleryUrls} (URLs, never stored).
 */
public record ExperienceResponse(
        UUID id,
        String context,
        String name,
        String slug,
        String description,
        String longDescription,
        boolean featured,
        List<String> tags,
        List<String> included,
        List<String> notIncluded,
        List<String> highlights,
        String thumbnailUrl,
        List<String> galleryUrls,
        int durationMinutes,
        int bookingCutoffHours,
        String status,
        UUID createdBy,
        Instant createdAt) {

    public static ExperienceResponse from(ExperienceView v) {
        return new ExperienceResponse(
                v.id(), "experiences", v.name(), v.slug(), v.description(), v.longDescription(),
                v.featured(), v.tags(), v.included(), v.notIncluded(), v.highlights(),
                v.thumbnailUrl(), v.galleryUrls(), v.durationMinutes(), v.bookingCutoffHours(),
                v.status(), v.createdBy(), v.createdAt());
    }
}
