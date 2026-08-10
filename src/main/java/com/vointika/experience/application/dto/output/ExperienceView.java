package com.vointika.experience.application.dto.output;

import com.vointika.experience.domain.entity.Experience;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * An experience for read APIs. Content flattened to primitives; media exposed
 * both as raw references ({@code thumbnailMediaId} + ordered {@code mediaIds},
 * so an editor can round-trip the selection) AND resolved to absolute URLs at
 * read time — {@code galleryUrls} follow the stored media-id order, and any id
 * that no longer resolves (deleted media) is dropped from the URLs (self-healing;
 * the raw {@code mediaIds} keep the full stored list).
 */
public record ExperienceView(
        UUID id,
        UUID tourOperatorId,
        UUID createdBy,
        String name,
        String handle,
        String description,
        String longDescription,
        boolean featured,
        UUID thumbnailMediaId,
        String thumbnailUrl,
        List<UUID> mediaIds,
        List<String> galleryUrls,
        int durationMinutes,
        int bookingCutoffHours,
        boolean published,
        BigDecimal startingPrice,
        Instant createdAt) {

    /** @param urlsById resolved media id → url (from MediaUrlBatchResolver). */
    public static ExperienceView from(Experience e, Map<UUID, String> urlsById) {
        List<String> gallery = e.getMediaIds().stream()
                .map(urlsById::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        String thumbnailUrl = e.getThumbnailMediaId() == null ? null : urlsById.get(e.getThumbnailMediaId());
        return new ExperienceView(
                e.getId(), e.getTourOperatorId(), e.getCreatedBy(),
                e.getName().value(), e.getHandle().value(), e.getDescription().value(),
                e.getLongDescription().value(), e.isFeatured(),
                e.getThumbnailMediaId(), thumbnailUrl, e.getMediaIds(), gallery,
                e.getDurationMinutes().value(), e.getBookingCutoffHours().value(),
                e.isPublished(), e.getStartingPrice().value(), e.getCreatedAt());
    }
}
