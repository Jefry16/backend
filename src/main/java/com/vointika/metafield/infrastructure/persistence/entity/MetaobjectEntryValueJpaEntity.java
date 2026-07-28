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
@Table(schema = "metafield", name = "metaobject_entry_values")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MetaobjectEntryValueJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID entryId;

    @Column(nullable = false, updatable = false)
    private UUID fieldDefinitionId;

    @Column(nullable = false, columnDefinition = "text")
    private String value;

    @Column(nullable = false, updatable = false)
    private UUID createdBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;
}
