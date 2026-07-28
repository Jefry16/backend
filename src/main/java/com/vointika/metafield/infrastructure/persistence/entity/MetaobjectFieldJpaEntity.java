package com.vointika.metafield.infrastructure.persistence.entity;

import com.vointika.metafield.domain.valueobject.MetafieldType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "metafield", name = "metaobject_field_definitions")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MetaobjectFieldJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID definitionId;

    @Column(name = "\"key\"", nullable = false, updatable = false, length = 64)
    private String key;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private MetafieldType type;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;
}
