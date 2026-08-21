package com.vointika.experience.infrastructure.persistence.entity;

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
@Table(schema = "experience", name = "category_translations")
@IdClass(CategoryTranslationId.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTranslationJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID categoryId;

    @Id
    @Column(nullable = false, updatable = false, length = 8)
    private String locale;

    @Column(nullable = false, updatable = false)
    private UUID tourOperatorId;

    @Column
    private String name;
}
