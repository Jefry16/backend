package com.vointika.media.presentation.response;

import com.vointika.media.application.dto.output.MediaView;
import com.vointika.shared.media.MediaUrlResolver;

import java.time.Instant;
import java.util.UUID;

/**
 * A media record for read APIs. {@code id} is the media id and {@code context}
 * is {@code "media"} (house convention). {@code url} is resolved from the stored
 * key at read time (never persisted). {@code uploadedBy} is the uploading user
 * (id + {@code context:"users"} + name), the name a snapshot frozen at upload
 * time (always present), mirroring the invitation {@code invitedBy}.
 */
public record MediaResponse(
        UUID id,
        String context,
        String url,
        String contentType,
        long sizeBytes,
        String originalName,
        Instant createdAt,
        UploadedBy uploadedBy) {

    public record UploadedBy(UUID id, String context, String name) {
        public UploadedBy(UUID id, String name) {
            this(id, "users", name);
        }
    }

    public static MediaResponse from(MediaView view, MediaUrlResolver urls) {
        return new MediaResponse(
                view.id(),
                "media",
                urls.toUrl(view.storageKey()),
                view.contentType(),
                view.sizeBytes(),
                view.originalName(),
                view.createdAt(),
                new UploadedBy(view.createdBy(), view.createdByName()));
    }
}
