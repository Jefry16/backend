package com.vointika.touroperator.infrastructure.persistence.entity;

import com.vointika.touroperator.domain.enums.PolicyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * One of the operator's legal documents. The primary key is
 * {@code (tour_operator_id, type)}, which is what says <b>one per type</b>
 * structurally rather than through a nullable discriminator and a partial unique
 * index.
 *
 * <p>Read-only throughout: there is no policy write path yet, and
 * {@code PolicyColumnsStayReadOnlyTest} is what says so at the moment one lands.
 *
 * <p>{@code type} maps as the enum rather than a string, so the two-lists
 * coupling with the CHECK constraint is a mapping and not a convention — and an
 * unknown value fails the read rather than reaching a theme.
 *
 * <p>{@code body} is raw HTML, stored verbatim, the way {@code pages.body} is.
 * Escaping is a render concern, and the storefront's policy template renders it
 * unescaped on purpose.
 */
@Entity
@Table(schema = "touroperator", name = "tour_operator_policies")
@IdClass(TourOperatorPolicyId.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TourOperatorPolicyJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID tourOperatorId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private PolicyType type;

    @Column(nullable = false, insertable = false, updatable = false, length = 200)
    private String title;

    @Column(nullable = false, insertable = false, updatable = false, columnDefinition = "text")
    private String body;

    @Column(nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;
}
