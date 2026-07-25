package com.vointika.pickup.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(schema = "pickup", name = "pickup_locations")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PickupLocationJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID tourOperatorId;

    @Column(nullable = false, updatable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalTime time;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
