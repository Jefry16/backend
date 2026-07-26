package com.vointika.audience.domain.entity;

import com.vointika.audience.domain.valueobject.AudienceName;
import com.vointika.audience.domain.valueobject.PaxPerUnit;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * An operator's pax pricing tier (Adults, Children, "VIP Table for 6", …).
 * Operator-scoped and reused across many experiences' slots — a slot references
 * an audience by id and snapshots its name + price at that time, so audiences and
 * slot pricing are separate concerns (see the slot context).
 */
public class Audience {

    private final UUID id;
    private final UUID tourOperatorId;
    private AudienceName name;
    private PaxPerUnit paxPerUnit;
    private final UUID createdBy;
    private final Instant createdAt;

    /** New audience. */
    public Audience(UUID id, UUID tourOperatorId, AudienceName name, PaxPerUnit paxPerUnit, UUID createdBy) {
        this(id, tourOperatorId, name, paxPerUnit, createdBy, Instant.now());
    }

    /** Reconstitution from persistence. */
    public Audience(UUID id,
                    UUID tourOperatorId,
                    AudienceName name,
                    PaxPerUnit paxPerUnit,
                    UUID createdBy,
                    Instant createdAt) {
        this.id = id;
        this.tourOperatorId = tourOperatorId;
        this.name = name;
        this.paxPerUnit = paxPerUnit;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public void rename(AudienceName newName) {
        this.name = newName;
    }

    public void changePaxPerUnit(PaxPerUnit newValue) {
        this.paxPerUnit = newValue;
    }

    public UUID getId() { return id; }
    public UUID getTourOperatorId() { return tourOperatorId; }
    /**
     * The auditable fields as JSON-native values — the exposure guard for the
     * audit log's field diffs (only what appears here can enter the trail).
     */
    public Map<String, Object> auditSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("name", name.value());
        snapshot.put("paxPerUnit", paxPerUnit.value());
        return snapshot;
    }

    public AudienceName getName() { return name; }
    public PaxPerUnit getPaxPerUnit() { return paxPerUnit; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
