package com.vointika.audit.domain.entity;

import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.FieldChange;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * One immutable audit-trail entry: who ({@link AuditActor} + the frozen
 * {@code actorName}) did what ({@code action}) to which entity
 * ({@code entityType}/{@code entityId}), when, with optional action-specific
 * {@code details} and an optional field-level {@code changes} diff.
 * Append-only by construction — no setters, no update semantics anywhere in
 * the context.
 */
public class AuditLogEntry {

    private static final int ENTITY_TYPE_MAX = 40;
    private static final int ACTION_MAX = 80;
    private static final int ACTOR_NAME_MAX = 255;
    private static final int REQUEST_ID_MAX = 64;

    private final UUID id;
    private final UUID tourOperatorId;
    private final AuditActor actor;
    private final String actorName;
    private final String entityType;
    private final UUID entityId;
    private final String action;
    private final Map<String, Object> details;
    private final List<FieldChange> changes;
    private final String requestId;
    private final Instant createdAt;

    private AuditLogEntry(UUID id, UUID tourOperatorId, AuditActor actor, String actorName,
                          String entityType, UUID entityId, String action,
                          Map<String, Object> details, List<FieldChange> changes,
                          String requestId, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.tourOperatorId = Objects.requireNonNull(tourOperatorId, "tourOperatorId must not be null");
        this.actor = Objects.requireNonNull(actor, "actor must not be null");
        this.actorName = requireBoundedOrNull(actorName, "actorName", ACTOR_NAME_MAX);
        this.entityType = requireBounded(entityType, "entityType", ENTITY_TYPE_MAX);
        this.entityId = Objects.requireNonNull(entityId, "entityId must not be null");
        this.action = requireBounded(action, "action", ACTION_MAX);
        this.details = details == null ? null : Map.copyOf(details);
        // Empty diff normalizes to null — a pure event stores no changes payload.
        this.changes = (changes == null || changes.isEmpty()) ? null : List.copyOf(changes);
        this.requestId = requireBoundedOrNull(requestId, "requestId", REQUEST_ID_MAX);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    /**
     * Records a new entry. {@code actorName}, {@code details}, {@code changes},
     * and {@code requestId} are all optional (a {@code null}/empty
     * {@code changes} stores no field diff).
     */
    public static AuditLogEntry record(UUID id, UUID tourOperatorId, AuditActor actor, String actorName,
                                       String entityType, UUID entityId, String action,
                                       Map<String, Object> details, List<FieldChange> changes,
                                       String requestId, Instant createdAt) {
        return new AuditLogEntry(id, tourOperatorId, actor, actorName, entityType, entityId, action,
                details, changes, requestId, createdAt);
    }

    private static String requireBounded(String value, String name, int max) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        if (value.length() > max) {
            throw new IllegalArgumentException(name + " must not exceed " + max + " characters");
        }
        return value;
    }

    private static String requireBoundedOrNull(String value, String name, int max) {
        if (value != null && value.length() > max) {
            throw new IllegalArgumentException(name + " must not exceed " + max + " characters");
        }
        return value;
    }

    public UUID getId() { return id; }
    public UUID getTourOperatorId() { return tourOperatorId; }
    public AuditActor getActor() { return actor; }
    public String getActorName() { return actorName; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public String getAction() { return action; }
    public Map<String, Object> getDetails() { return details; }
    public List<FieldChange> getChanges() { return changes; }
    public String getRequestId() { return requestId; }
    public Instant getCreatedAt() { return createdAt; }
}
