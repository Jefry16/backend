package com.vointika.page.infrastructure.persistence.entity;

import com.vointika.page.domain.enums.PageStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "page", name = "pages")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PageJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID tourOperatorId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 170)
    private String handle;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(length = 70)
    private String seoTitle;

    @Column(length = 320)
    private String seoDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PageStatus status;

    @Column(length = 170)
    private String templateSuffix;

    @Column(nullable = false, updatable = false)
    private UUID createdBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;
}
