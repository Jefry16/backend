package com.vointika.media.domain.entity;

import com.vointika.media.domain.valueobject.MediaAlt;

import java.time.Instant;
import java.util.UUID;

/**
 * One uploaded file owned by a tour operator (the tenant). The row holds only a
 * relative {@code storageKey} — never an absolute URL — so the asset domain can
 * change without a data migration; the URL is built at read time from the key.
 * Referenced elsewhere by media id (experiences, operator logo), resolved
 * id→key→url through the media context's cross-context port.
 */
public class Media {

    private final UUID id;
    private final UUID tourOperatorId;
    private final String storageKey;
    private final String contentType;
    private final long sizeBytes;
    private final String originalName;
    private final UUID createdBy;
    private final String createdByName;
    private final Instant createdAt;

    /**
     * Alt is mutable and the dimensions are not: one is a description the
     * uploader supplies afterwards, the other two are facts about the bytes that
     * were stored, and replacing the bytes means uploading a new file.
     */
    private MediaAlt alt;
    private final Integer width;
    private final Integer height;

    /** A freshly uploaded file, stamped now. */
    public static Media upload(UUID id,
                               UUID tourOperatorId,
                               String storageKey,
                               String contentType,
                               long sizeBytes,
                               String originalName,
                               UUID createdBy,
                               String createdByName,
                               Integer width,
                               Integer height) {
        // No alt at upload: only the person who chose the image can write it, and
        // the upload is a file, not a description.
        return new Media(id, tourOperatorId, storageKey, contentType, sizeBytes,
                originalName, createdBy, createdByName, Instant.now(), null, width, height);
    }

    // Reconstitution from persistence.
    public Media(UUID id,
                 UUID tourOperatorId,
                 String storageKey,
                 String contentType,
                 long sizeBytes,
                 String originalName,
                 UUID createdBy,
                 String createdByName,
                 Instant createdAt,
                 MediaAlt alt,
                 Integer width,
                 Integer height) {
        this.id = id;
        this.tourOperatorId = tourOperatorId;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.originalName = originalName;
        this.createdBy = createdBy;
        this.createdByName = createdByName;
        this.createdAt = createdAt;
        this.alt = alt;
        this.width = width;
        this.height = height;
    }

    /** Sets or clears the alt text. Null clears it — an image may legitimately have none. */
    public void describe(MediaAlt newAlt) {
        this.alt = newAlt;
    }

    public UUID getId() { return id; }
    public UUID getTourOperatorId() { return tourOperatorId; }
    public String getStorageKey() { return storageKey; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getOriginalName() { return originalName; }
    public UUID getCreatedBy() { return createdBy; }
    public String getCreatedByName() { return createdByName; }
    public MediaAlt getAlt() { return alt; }
    public Integer getWidth() { return width; }
    public Integer getHeight() { return height; }
    public Instant getCreatedAt() { return createdAt; }
}
