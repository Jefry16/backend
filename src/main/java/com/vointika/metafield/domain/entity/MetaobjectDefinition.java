package com.vointika.metafield.domain.entity;

import com.vointika.metafield.domain.valueobject.MetafieldDefinitionName;
import com.vointika.metafield.domain.valueobject.MetafieldDescription;
import com.vointika.metafield.domain.valueobject.MetaobjectType;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The blueprint of one custom content type (e.g. a "size chart" or "guide
 * profile"): a type slug plus ordered field definitions
 * ({@link MetaobjectField}). {@code type} is IMMUTABLE after create — entries
 * and (later) themes address the definition through it. Only the display name
 * and description are editable.
 */
public class MetaobjectDefinition {

    private final UUID id;
    private final UUID tourOperatorId;
    private final MetaobjectType type;
    private MetafieldDefinitionName name;
    private MetafieldDescription description;
    private final UUID createdBy;
    private final Instant createdAt;
    private Instant updatedAt;

    // Constructor for creating a brand new definition
    public MetaobjectDefinition(UUID id,
                                UUID tourOperatorId,
                                MetaobjectType type,
                                MetafieldDefinitionName name,
                                MetafieldDescription description,
                                UUID createdBy) {
        this.id = id;
        this.tourOperatorId = tourOperatorId;
        this.type = type;
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // Constructor for reconstituting from persistence
    public MetaobjectDefinition(UUID id,
                                UUID tourOperatorId,
                                MetaobjectType type,
                                MetafieldDefinitionName name,
                                MetafieldDescription description,
                                UUID createdBy,
                                Instant createdAt,
                                Instant updatedAt) {
        this.id = id;
        this.tourOperatorId = tourOperatorId;
        this.type = type;
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Full replace of the editable attributes. A {@code null} description clears it. */
    public void update(MetafieldDefinitionName newName, MetafieldDescription newDescription) {
        this.name = newName;
        this.description = newDescription;
        this.updatedAt = Instant.now();
    }

    /** The audit-worthy fields — the two MUTABLE ones (type identifies, rides details). */
    public Map<String, Object> auditSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("name", name.value());
        snapshot.put("description", description == null ? null : description.value());
        return snapshot;
    }

    public UUID getId() { return id; }
    public UUID getTourOperatorId() { return tourOperatorId; }
    public MetaobjectType getType() { return type; }
    public MetafieldDefinitionName getName() { return name; }
    public Optional<MetafieldDescription> getDescription() { return Optional.ofNullable(description); }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
