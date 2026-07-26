package com.vointika.audit.infrastructure.persistence.entity;

import com.vointika.shared.valueobject.AuditActorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only: every column is {@code updatable = false}; the application
 * never updates or deletes rows.
 */
@Entity
@Table(schema = "audit", name = "audit_log")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID tourOperatorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private AuditActorType actorType;

    @Column(updatable = false)
    private UUID actorId;

    // Frozen at write time — the name as it was when the action happened.
    @Column(updatable = false, length = 255)
    private String actorName;

    @Column(nullable = false, updatable = false, length = 40)
    private String entityType;

    @Column(nullable = false, updatable = false)
    private UUID entityId;

    @Column(nullable = false, updatable = false, length = 80)
    private String action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(updatable = false, columnDefinition = "jsonb")
    private String details;

    // Field-level change history: a JSON array of {field, from, to} (or null).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(updatable = false, columnDefinition = "jsonb")
    private String changes;

    @Column(updatable = false, length = 64)
    private String requestId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
