package com.vointika.metafield.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "metafield", name = "metaobject_entries")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MetaobjectEntryJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID tourOperatorId;

    @Column(nullable = false, updatable = false)
    private UUID definitionId;

    @Column(nullable = false, length = 170)
    private String handle;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private boolean published;

    @Column(nullable = false, updatable = false)
    private UUID createdBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;
}
