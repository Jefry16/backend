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
 * <p><b>Writable since the brand write path landed.</b> Every field was mapped
 * read-only while the only writer was the logo endpoint: the repository rebuilt
 * this entity from the one value it wrote and had nothing to put in the rest, so
 * leaving them writable meant setting a logo issued an UPDATE nulling the slogan,
 * the description and the three other images. The domain {@code Brand} carries
 * all of them now, {@code PUT .../brand} replaces the whole object, and
 * {@code BrandColumnsStayReadOnlyTest} still pins the biconditional in both
 * directions — a column is writable exactly while the domain can carry it.
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

    @Column(length = 80)
    private String slogan;

    @Column(length = 150)
    private String shortDescription;

    /**
     * The one writable column: it carries the logo V10 moved off
     * {@code tour_operators}, and {@code PUT}/{@code DELETE .../logo} followed it
     * here rather than being deleted with its old home.
     */
    @Column
    private UUID logoMediaId;

    @Column
    private UUID squareLogoMediaId;

    @Column
    private UUID faviconMediaId;

    @Column
    private UUID coverImageMediaId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;
}
