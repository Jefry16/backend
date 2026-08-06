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

import java.util.UUID;

/**
 * A policy in one of the operator's other locales. <b>Both content columns are
 * nullable and that is the mechanism</b>: a row overlays the canonical policy
 * column by column, so translating the title without the body is a title in
 * Spanish over an English body rather than a blank page.
 *
 * <p>The third table in this codebase doing exactly that (PATTERNS §2a).
 *
 * <p>Read-only throughout, like the policy itself.
 */
@Entity
@Table(schema = "touroperator", name = "tour_operator_policy_translations")
@IdClass(TourOperatorPolicyTranslationId.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TourOperatorPolicyTranslationJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID tourOperatorId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private PolicyType type;

    @Id
    @Column(nullable = false, updatable = false, length = 8)
    private String locale;

    @Column(insertable = false, updatable = false, length = 200)
    private String title;

    @Column(insertable = false, updatable = false, columnDefinition = "text")
    private String body;
}
