package com.vointika.media.application.dto.output;

import com.vointika.media.domain.entity.Media;

import java.time.Instant;
import java.util.UUID;

/**
 * A media record for read APIs. Carries the raw {@code storageKey} (the
 * presentation layer resolves it to an absolute URL) plus the uploader's id and
 * display name — a snapshot frozen onto the row at upload time (always present,
 * stable even if the uploader is later renamed or removed).
 */
public record MediaView(
        UUID id,
        String storageKey,
        String contentType,
        long sizeBytes,
        String originalName,
        Instant createdAt,
        UUID createdBy,
        String createdByName) {

    public static MediaView from(Media media) {
        return new MediaView(
                media.getId(),
                media.getStorageKey(),
                media.getContentType(),
                media.getSizeBytes(),
                media.getOriginalName(),
                media.getCreatedAt(),
                media.getCreatedBy(),
                media.getCreatedByName());
    }
}
