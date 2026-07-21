package com.vointika.experience.application.dto.output;

import com.vointika.experience.domain.entity.Experience;
import com.vointika.experience.domain.valueobject.Highlight;
import com.vointika.experience.domain.valueobject.InclusionItem;
import com.vointika.experience.domain.valueobject.Tag;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * An experience for read APIs. Content flattened to primitives; media resolved
 * to absolute URLs at read time — {@code galleryUrls} follow the stored media-id
 * order, and any id that no longer resolves (deleted media) is dropped
 * (self-healing).
 */
public record ExperienceView(
        UUID id,
        UUID tourOperatorId,
        UUID createdBy,
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
                e.getName().value(), e.getSlug().value(), e.getDescription().value(),
                e.getLongDescription().value(), e.isFeatured(),
                e.getTags().stream().map(Tag::value).toList(),
                e.getIncluded().stream().map(InclusionItem::value).toList(),
                e.getNotIncluded().stream().map(InclusionItem::value).toList(),
                e.getHighlights().stream().map(Highlight::value).toList(),
                thumbnailUrl, gallery,
                e.getDurationMinutes().value(), e.getBookingCutoffHours().value(),
                e.getStatus().name(), e.getCreatedAt());
    }
}
