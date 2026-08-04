package com.vointika.touroperator.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * The operator's brand — Shopify's {@code shop.brand}, one row per operator, in
 * its own table rather than seven more columns on an already 22-wide
 * {@code tour_operators}.
 *
 * <p><b>Everything but the logo is mapped read-only, and the flags are the
 * whole guard.</b> There is no write path for the brand's own fields yet, so
 * {@code TourOperatorBrandRepositoryImpl} rebuilds this entity from the one
 * value it does write and has nothing to put in the rest. Left writable, that
 * means setting a logo issues an UPDATE nulling the slogan, the description and
 * the three other images. {@code insertable/updatable = false} keeps them out of
 * every INSERT and UPDATE Hibernate generates. The slice that adds the brand
 * write path flips them, together with the repository method that writes each —
 * which is exactly what {@code BrandColumnsStayReadOnlyTest} asserts.
 *
 * <p>The four media references are bare ids with no FK, for the reason V3 gives
 * for the logo: media FKs <em>into</em> touroperator, so a reverse FK would be
 * circular and would invert the Flyway domain order.
 */
@Entity
@Table(schema = "touroperator", name = "tour_operator_brand")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TourOperatorBrandJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID tourOperatorId;

    @Column(length = 80, insertable = false, updatable = false)
    private String slogan;

    @Column(length = 150, insertable = false, updatable = false)
    private String shortDescription;

    /**
     * The one writable column: it carries the logo V10 moved off
     * {@code tour_operators}, and {@code PUT}/{@code DELETE .../logo} followed it
     * here rather than being deleted with its old home.
     */
    @Column
    private UUID logoMediaId;

    @Column(insertable = false, updatable = false)
    private UUID squareLogoMediaId;

    @Column(insertable = false, updatable = false)
    private UUID faviconMediaId;

    @Column(insertable = false, updatable = false)
    private UUID coverImageMediaId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;
}
