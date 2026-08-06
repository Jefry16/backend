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
     * The brand's translatable text (V10). <b>Writable since the domain started
     * carrying it</b> — these were mapped {@code insertable/updatable = false}
     * while {@code TourOperatorTranslation} had no field for them, because a
     * writable column with no domain value means every SEO edit rebuilds the row
     * with a null and blanks the brand.
     *
     * <p>{@code BrandColumnsStayReadOnlyTest} pins that as a biconditional in
     * both directions, so putting either flag back without also removing the
     * domain accessor fails the build — as does the reverse.
     */
    @Column(length = 80)
    private String slogan;

    @Column(length = 150)
    private String shortDescription;
}
