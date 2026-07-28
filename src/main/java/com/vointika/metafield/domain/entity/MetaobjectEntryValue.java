package com.vointika.metafield.domain.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * One entry's stored value for one field — at most one per (entry, field),
 * already normalized by the validator for the field's type. An unset field
 * simply has no row.
 */
public class MetaobjectEntryValue {

    private final UUID id;
    private final UUID entryId;
    private final UUID fieldDefinitionId;
    private String value;
    private final UUID createdBy;
    private final Instant createdAt;
    private Instant updatedAt;

    // Constructor for creating a brand new value
    public MetaobjectEntryValue(UUID id, UUID entryId, UUID fieldDefinitionId,
                                String value, UUID createdBy) {
        this.id = id;
        this.entryId = entryId;
        this.fieldDefinitionId = fieldDefinitionId;
        this.value = value;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // Constructor for reconstituting from persistence
    public MetaobjectEntryValue(UUID id, UUID entryId, UUID fieldDefinitionId,
                                String value, UUID createdBy,
                                Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.entryId = entryId;
        this.fieldDefinitionId = fieldDefinitionId;
        this.value = value;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void changeValue(String newValue) {
        this.value = newValue;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getEntryId() { return entryId; }
    public UUID getFieldDefinitionId() { return fieldDefinitionId; }
    public String getValue() { return value; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
