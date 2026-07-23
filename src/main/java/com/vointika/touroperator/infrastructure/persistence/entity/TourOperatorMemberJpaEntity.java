package com.vointika.touroperator.infrastructure.persistence.entity;

import com.vointika.touroperator.domain.enums.MemberRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "touroperator", name = "tour_operator_members")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TourOperatorMemberJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID tourOperatorId;

    @Column(nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(nullable = false, updatable = false)
    private Instant joinedAt;

    // Denormalized read-fields (see V1 members-table comment). Updatable — an
    // identity name/email change syncs onto the row.
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;
}
