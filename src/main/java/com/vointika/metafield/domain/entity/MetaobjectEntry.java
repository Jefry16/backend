package com.vointika.metafield.domain.entity;

import com.vointika.metafield.domain.valueobject.MetaobjectEntryName;
import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.valueobject.Handle;

import java.time.Instant;
import java.util.UUID;

/**
 * One piece of content of a metaobject type (e.g. THE "beginner size chart"
 * entry of the "size chart" definition). Carries an operator-chosen handle
 * (unique per definition) + display name; the field values live in
 * {@link MetaobjectEntryValue} rows. Created unpublished; {@code published}
 * is a plain boolean flipped by publish/unpublish (the experience model,
 * not the pages status enum).
 */
public class MetaobjectEntry {

    /** Public because both are published: publishing and unpublishing are each refused from the wrong state. */
    public static final String ALREADY_PUBLISHED = "Metaobject is already published";
    public static final String NOT_PUBLISHED = "Metaobject is not published";

    private final UUID id;
    private final UUID tourOperatorId;
    private final UUID definitionId;
    private Handle handle;
    private MetaobjectEntryName name;
    private boolean published;
    private final UUID createdBy;
    private final Instant createdAt;
    private Instant updatedAt;

    // Constructor for creating a brand new entry (always unpublished)
    public MetaobjectEntry(UUID id,
                           UUID tourOperatorId,
                           UUID definitionId,
                           Handle handle,
                           MetaobjectEntryName name,
                           UUID createdBy) {
        this.id = id;
        this.tourOperatorId = tourOperatorId;
        this.definitionId = definitionId;
        this.handle = handle;
        this.name = name;
        this.published = false;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // Constructor for reconstituting from persistence
    public MetaobjectEntry(UUID id,
                           UUID tourOperatorId,
                           UUID definitionId,
                           Handle handle,
                           MetaobjectEntryName name,
                           boolean published,
                           UUID createdBy,
                           Instant createdAt,
                           Instant updatedAt) {
        this.id = id;
        this.tourOperatorId = tourOperatorId;
        this.definitionId = definitionId;
        this.handle = handle;
        this.name = name;
        this.published = published;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Full replace of name + handle (handle uniqueness is the caller's concern). */
    public void update(MetaobjectEntryName newName, Handle newHandle) {
        this.name = newName;
        this.handle = newHandle;
        this.updatedAt = Instant.now();
    }

    public void publish() {
        if (published) {
            throw new ConflictException(ALREADY_PUBLISHED);
        }
        this.published = true;
        this.updatedAt = Instant.now();
    }

    public void unpublish() {
        if (!published) {
            throw new ConflictException(NOT_PUBLISHED);
        }
        this.published = false;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTourOperatorId() { return tourOperatorId; }
    public UUID getDefinitionId() { return definitionId; }
    public Handle getHandle() { return handle; }
    public MetaobjectEntryName getName() { return name; }
    public boolean isPublished() { return published; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
