package com.vointika.audit.presentation.response;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.vointika.audit.domain.projection.AuditLogListItem;

import java.time.Instant;
import java.util.UUID;

public record AuditLogEntryResponse(
        UUID id,
        String context,
        String actorType,
        UUID actorId,
        // The acting user's display name, FROZEN at write time (as they were
        // known when the action happened); null for SYSTEM.
        String actorName,
        String entityType,
        UUID entityId,
        String action,
        // Stored JSON emitted verbatim — an object (or null), never a quoted string
        @JsonRawValue String details,
        // Field-level change history — a JSON array of {field, from, to} (or null)
        @JsonRawValue String changes,
        String requestId,
        Instant createdAt
) {

    public static AuditLogEntryResponse from(AuditLogListItem item) {
        return new AuditLogEntryResponse(
                item.id(),
                "audit-log-entries",
                item.actorType().name(),
                item.actorId(),
                item.actorName(),
                item.entityType(),
                item.entityId(),
                item.action(),
                item.detailsJson(),
                item.changesJson(),
                item.requestId(),
                item.createdAt()
        );
    }
}
