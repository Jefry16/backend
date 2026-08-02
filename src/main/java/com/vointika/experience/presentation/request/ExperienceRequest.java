package com.vointika.experience.presentation.request;

import java.util.List;
import java.util.UUID;

/**
 * The create/update body for an experience. {@code featured} defaults to false;
 * null lists are treated as empty. Slug and status are not settable here (slug
 * is generated + immutable; status changes via publish/unpublish).
 */
public record ExperienceRequest(
        String name,
        String description,
        String longDescription,
        Boolean featured,
        List<String> tags,
        List<String> included,
        List<String> notIncluded,
        List<String> highlights,
        List<UUID> mediaIds,
        UUID thumbnailMediaId,
        Integer durationMinutes,
        Integer bookingCutoffHours,
        String seoTitle,
        String seoDescription) {
}
