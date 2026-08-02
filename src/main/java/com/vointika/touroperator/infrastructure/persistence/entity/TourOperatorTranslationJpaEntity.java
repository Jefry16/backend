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
}
