package com.vointika.experience.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(schema = "experience", name = "experiences")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID tourOperatorId;

    @Column(nullable = false, updatable = false)
    private UUID createdBy;

    @Column(nullable = false, updatable = false)
    private String handle;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, columnDefinition = "text")
    private String longDescription;

    @Column(nullable = false)
    private boolean featured;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private List<String> tags;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private List<String> included;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private List<String> notIncluded;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private List<String> highlights;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "uuid[]")
    private List<UUID> mediaIds;

    @Column
    private UUID thumbnailMediaId;

    @Column(nullable = false)
    private int durationMinutes;

    @Column(nullable = false)
    private int bookingCutoffHours;

    @Column(nullable = false)
    private boolean published;

    @Column(length = 70)
    private String seoTitle;

    @Column(length = 320)
    private String seoDescription;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal startingPrice;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
