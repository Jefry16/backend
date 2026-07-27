package com.vointika.metafield.domain.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * One definition's content on one owning resource instance (at most one row
 * per (definition, owner)). {@code value} is the canonical string form the
 * validator produced; the definition's type says how to interpret it.
 */
public class MetafieldValue {

    private final UUID id;
    private final UUID definitionId;
    private final UUID ownerId;
    private String value;
    private final UUID createdBy;
    private final Instant createdAt;
    private Instant updatedAt;

    // Constructor for creating a brand new value
    public MetafieldValue(UUID id, UUID definitionId, UUID ownerId, String value, UUID createdBy) {
        this.id = id;
        this.definitionId = definitionId;
        this.ownerId = ownerId;
        this.value = value;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // Constructor for reconstituting from persistence
    public MetafieldValue(UUID id, UUID definitionId, UUID ownerId, String value,
                          UUID createdBy, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.definitionId = definitionId;
        this.ownerId = ownerId;
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
    public UUID getDefinitionId() { return definitionId; }
    public UUID getOwnerId() { return ownerId; }
    public String getValue() { return value; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
