package com.vointika.touroperator.infrastructure.persistence.entity;

import com.vointika.touroperator.domain.enums.BrandSocialPlatform;
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
 * One social link of the brand. Shopify carries these on
 * {@code brand.metafields}; ours is a first-class child table, because a
 * merchant-authored metafield definition is not a contract a theme can rely on
 * — every {@code metaobject_definitions} row here is operator-authored and none
 * is guaranteed to exist.
 *
 * <p>Read-only throughout: there is no brand write path yet.
 *
 * <p>{@code platform} maps as the enum, so the two-lists coupling with the CHECK
 * constraint is a mapping and not a convention — and an unknown value fails at
 * the read rather than reaching a theme.
 */
@Entity
@Table(schema = "touroperator", name = "tour_operator_brand_social_links")
@IdClass(TourOperatorBrandSocialLinkId.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TourOperatorBrandSocialLinkJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID tourOperatorId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private BrandSocialPlatform platform;

    @Column(nullable = false, length = 500)
    private String url;
}
