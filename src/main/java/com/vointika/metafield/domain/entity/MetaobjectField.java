package com.vointika.metafield.domain.entity;

import com.vointika.metafield.domain.valueobject.MetafieldDefinitionName;
import com.vointika.metafield.domain.valueobject.MetafieldKey;
import com.vointika.metafield.domain.valueobject.MetafieldType;

import java.time.Instant;
import java.util.UUID;

/**
 * One field of a metaobject definition — key + type from the shared metafield
 * type catalogue, unique per definition, ordered by {@code position}.
 * {@code key} and {@code type} are IMMUTABLE (entry values are addressed and
 * validated through them); only the display name is editable.
 */
public class MetaobjectField {

    private final UUID id;
    private final UUID definitionId;
    private final MetafieldKey key;
    private final MetafieldType type;
    private MetafieldDefinitionName name;
    private final int position;
    private final Instant createdAt;
    private Instant updatedAt;

    // Constructor for creating a brand new field
    public MetaobjectField(UUID id,
                           UUID definitionId,
                           MetafieldKey key,
                           MetafieldType type,
                           MetafieldDefinitionName name,
                           int position) {
        this.id = id;
        this.definitionId = definitionId;
        this.key = key;
        this.type = type;
        this.name = name;
        this.position = position;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // Constructor for reconstituting from persistence
    public MetaobjectField(UUID id,
                           UUID definitionId,
                           MetafieldKey key,
                           MetafieldType type,
                           MetafieldDefinitionName name,
                           int position,
                           Instant createdAt,
                           Instant updatedAt) {
        this.id = id;
        this.definitionId = definitionId;
        this.key = key;
        this.type = type;
        this.name = name;
        this.position = position;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void rename(MetafieldDefinitionName newName) {
        this.name = newName;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getDefinitionId() { return definitionId; }
    public MetafieldKey getKey() { return key; }
    public MetafieldType getType() { return type; }
    public MetafieldDefinitionName getName() { return name; }
    public int getPosition() { return position; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
