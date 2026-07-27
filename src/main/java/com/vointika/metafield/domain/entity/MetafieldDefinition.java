package com.vointika.metafield.domain.entity;

import com.vointika.metafield.domain.valueobject.MetafieldDefinitionName;
import com.vointika.metafield.domain.valueobject.MetafieldDescription;
import com.vointika.metafield.domain.valueobject.MetafieldKey;
import com.vointika.metafield.domain.valueobject.MetafieldNamespace;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.metafield.domain.valueobject.MetafieldType;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The schema of one custom field an operator attaches to a resource kind
 * (e.g. "experiences carry a {@code custom.difficulty} single-line text").
 * Identified by {@code namespace.key}, unique per (tour operator, owner type).
 *
 * <p>{@code ownerType}, {@code namespace}, {@code key} and {@code type} are
 * IMMUTABLE after creation — stored values are validated and addressed
 * through them, so changing them would silently orphan or re-interpret
 * existing data. Only the display name and description are editable.
 */
public class MetafieldDefinition {

    private final UUID id;
    private final UUID tourOperatorId;
    private final MetafieldOwnerType ownerType;
    private final MetafieldNamespace namespace;
    private final MetafieldKey key;
    private final MetafieldType type;
    private MetafieldDefinitionName name;
    private MetafieldDescription description;
    private final UUID createdBy;
    private final Instant createdAt;
    private Instant updatedAt;

    // Constructor for creating a brand new definition
    public MetafieldDefinition(UUID id,
                               UUID tourOperatorId,
                               MetafieldOwnerType ownerType,
                               MetafieldNamespace namespace,
                               MetafieldKey key,
                               MetafieldType type,
                               MetafieldDefinitionName name,
                               MetafieldDescription description,
                               UUID createdBy) {
        this.id = id;
        this.tourOperatorId = tourOperatorId;
        this.ownerType = ownerType;
        this.namespace = namespace;
        this.key = key;
        this.type = type;
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // Constructor for reconstituting from persistence
    public MetafieldDefinition(UUID id,
                               UUID tourOperatorId,
                               MetafieldOwnerType ownerType,
                               MetafieldNamespace namespace,
                               MetafieldKey key,
                               MetafieldType type,
                               MetafieldDefinitionName name,
                               MetafieldDescription description,
                               UUID createdBy,
                               Instant createdAt,
                               Instant updatedAt) {
        this.id = id;
        this.tourOperatorId = tourOperatorId;
        this.ownerType = ownerType;
        this.namespace = namespace;
        this.key = key;
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

    /**
     * The audit-worthy fields — the two MUTABLE ones (ownerType/namespace/key/
     * type are final and identify rather than change; they ride details).
     */
    public Map<String, Object> auditSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("name", name.value());
        snapshot.put("description", description == null ? null : description.value());
        return snapshot;
    }

    public UUID getId() { return id; }
    public UUID getTourOperatorId() { return tourOperatorId; }
    public MetafieldOwnerType getOwnerType() { return ownerType; }
    public MetafieldNamespace getNamespace() { return namespace; }
    public MetafieldKey getKey() { return key; }
    public MetafieldType getType() { return type; }
    public MetafieldDefinitionName getName() { return name; }
    public Optional<MetafieldDescription> getDescription() { return Optional.ofNullable(description); }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
