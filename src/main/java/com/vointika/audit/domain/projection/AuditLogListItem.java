package com.vointika.audit.domain.projection;

import com.vointika.shared.valueobject.AuditActorType;

import java.time.Instant;
import java.util.UUID;

/**
 * One stored audit row as READ — {@code details}/{@code changes} stay raw JSON
 * strings, passed through to the response verbatim ({@code @JsonRawValue});
 * the read side never deserializes what the write side serialized.
 */
public record AuditLogListItem(
        UUID id,
        AuditActorType actorType,
        UUID actorId,
        String actorName,
        String entityType,
        UUID entityId,
        String action,
        String detailsJson,
        String changesJson,
        String requestId,
        Instant createdAt) {
}
