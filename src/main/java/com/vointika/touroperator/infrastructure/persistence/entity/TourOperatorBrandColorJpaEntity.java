package com.vointika.touroperator.infrastructure.persistence.entity;

import com.vointika.touroperator.domain.enums.BrandColorRole;
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

import java.util.UUID;

/**
 * One colour of the brand palette. {@code role} plus an ordered {@code position}
 * is what makes the palette faithful to Shopify's
 * {@code colors.primary[0].background} — a role is a <em>list</em>, not a value.
 *
 * <p>Read-only throughout: there is no brand write path yet, and
 * {@code BrandColumnsStayReadOnlyTest} is what says so at the moment one lands.
 *
 * <p>{@code role} maps as the enum rather than a string, so the two-lists
 * coupling with the CHECK constraint is a mapping and not a convention.
 */
@Entity
@Table(schema = "touroperator", name = "tour_operator_brand_colors")
@IdClass(TourOperatorBrandColorId.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TourOperatorBrandColorJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID tourOperatorId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 9)
    private BrandColorRole role;

    @Id
    @Column(nullable = false, updatable = false)
    private short position;

    @Column(nullable = false, insertable = false, updatable = false, length = 7)
    private String background;

    @Column(nullable = false, insertable = false, updatable = false, length = 7)
    private String foreground;
}
