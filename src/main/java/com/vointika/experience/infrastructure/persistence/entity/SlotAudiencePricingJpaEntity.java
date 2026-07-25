package com.vointika.experience.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(schema = "experience", name = "audience_slot")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SlotAudiencePricingJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID slotId;

    @Column(nullable = false, updatable = false)
    private UUID audienceId;

    @Column(nullable = false)
    private String audienceName;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private int paxPerUnit;

    @Column(nullable = false)
    private int bookedCount;
}
