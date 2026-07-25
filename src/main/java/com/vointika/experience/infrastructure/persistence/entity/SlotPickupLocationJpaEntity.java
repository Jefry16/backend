package com.vointika.experience.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(schema = "experience", name = "pickup_location_slot")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SlotPickupLocationJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID slotId;

    @Column(nullable = false, updatable = false)
    private UUID pickupLocationId;

    // Synced snapshots — refreshed by the pickup-catalog propagation.
    @Column(nullable = false)
    private String pickupLocationName;

    @Column(nullable = false)
    private LocalTime pickupLocationTime;
}
