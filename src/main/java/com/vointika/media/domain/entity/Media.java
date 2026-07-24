package com.vointika.media.domain.entity;

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

    /** A freshly uploaded file, stamped now. */
    public static Media upload(UUID id,
                               UUID tourOperatorId,
                               String storageKey,
                               String contentType,
                               long sizeBytes,
                               String originalName,
                               UUID createdBy,
                               String createdByName) {
        return new Media(id, tourOperatorId, storageKey, contentType, sizeBytes,
                originalName, createdBy, createdByName, Instant.now());
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
                 Instant createdAt) {
        this.id = id;
        this.tourOperatorId = tourOperatorId;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.originalName = originalName;
        this.createdBy = createdBy;
        this.createdByName = createdByName;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getTourOperatorId() { return tourOperatorId; }
    public String getStorageKey() { return storageKey; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getOriginalName() { return originalName; }
    public UUID getCreatedBy() { return createdBy; }
    public String getCreatedByName() { return createdByName; }
    public Instant getCreatedAt() { return createdAt; }
}
