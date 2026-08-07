package com.vointika.touroperator.infrastructure.persistence.entity;

import com.vointika.touroperator.domain.enums.PolicyType;
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

/**
 * One of the operator's legal documents. The primary key is a surrogate
 * {@code id} and <b>one per type</b> is said by the
 * {@code (tour_operator_id, type)} UNIQUE constraint — the {@code menus} shape.
 *
 * <p>V12 keyed this on the composite directly, which was right while nothing
 * listed it. The shared list framework keys its cursor on a UUID {@code id} and
 * puts that column in the ORDER BY of every query, so an entity without one
 * cannot be listed through it at all; V13 added the surrogate. The identity is
 * unchanged — it moved from the primary key to a constraint.
 *
 * <p>Writable since the admin write path landed. {@code created_at} and the two
 * key columns stay {@code updatable = false}: rewriting a policy keeps the date
 * it was first written, and retyping one would move it to another URL, which is
 * a delete and a create rather than an update.
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
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TourOperatorPolicyJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID tourOperatorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private PolicyType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;
}
