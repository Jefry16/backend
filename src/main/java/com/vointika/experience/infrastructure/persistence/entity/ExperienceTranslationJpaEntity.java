package com.vointika.experience.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

@Entity
@Table(schema = "experience", name = "experience_translations")
@IdClass(ExperienceTranslationId.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceTranslationJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID experienceId;

    @Id
    @Column(nullable = false, updatable = false, length = 8)
    private String locale;

    @Column(nullable = false, updatable = false)
    private UUID tourOperatorId;

    @Column
    private String name;

    @Column
    private String description;

    @Column(columnDefinition = "text")
    private String longDescription;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> highlights;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> included;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> notIncluded;

    @Column
    private String slug;

    @Column(length = 70)
    private String seoTitle;

    @Column(length = 320)
    private String seoDescription;
}
