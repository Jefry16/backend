package com.vointika.touroperator.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(schema = "touroperator", name = "tour_operator_translations")
@IdClass(TourOperatorTranslationId.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TourOperatorTranslationJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID tourOperatorId;

    @Id
    @Column(nullable = false, updatable = false, length = 8)
    private String locale;

    @Column(length = 70)
    private String seoTitle;

    @Column(length = 320)
    private String seoDescription;

    @Column(columnDefinition = "text")
    private String passwordMessage;

    /**
     * The brand's translatable text (V10), <b>mapped read-only on purpose</b> —
     * the shape {@code TourOperatorJpaEntity}'s phone and email have, for the
     * same reason.
     *
     * <p>The domain {@code TourOperatorTranslation} carries neither, so
     * {@code TourOperatorTranslationMapper.toJpa} rebuilds this entity with
     * nulls in their place. Left writable, that means the existing
     * {@code PUT .../translations/{locale}} — an SEO edit that never mentions the
     * brand — wipes both columns. {@code BrandColumnsStayReadOnlyTest} pins the
     * biconditional: read-only exactly while the domain cannot carry the value.
     */
    @Column(length = 80, insertable = false, updatable = false)
    private String slogan;

    @Column(length = 150, insertable = false, updatable = false)
    private String shortDescription;
}
