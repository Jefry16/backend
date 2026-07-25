package com.vointika.experience.infrastructure.persistence.entity;

import com.vointika.experience.domain.valueobject.SlotStatus;
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
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(schema = "experience", name = "slots")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SlotJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID experienceId;

    @Column(nullable = false, updatable = false)
    private UUID tourOperatorId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime startAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime endAt;

    @Column(nullable = false, updatable = false)
    private int day;

    // Synced snapshots — refreshed by the experience-update propagation, so not
    // updatable=false like the timing columns.
    @Column(nullable = false)
    private String experienceName;

    @Column(nullable = false)
    private String experienceDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SlotStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
