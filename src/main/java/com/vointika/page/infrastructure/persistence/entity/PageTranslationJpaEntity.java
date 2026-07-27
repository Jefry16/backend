package com.vointika.page.infrastructure.persistence.entity;

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
@Table(schema = "page", name = "page_translations")
@IdClass(PageTranslationId.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PageTranslationJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID pageId;

    @Id
    @Column(nullable = false, updatable = false, length = 8)
    private String locale;

    @Column(nullable = false, updatable = false)
    private UUID tourOperatorId;

    @Column(length = 170)
    private String slug;

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String body;

    @Column(length = 70)
    private String seoTitle;

    @Column(length = 320)
    private String seoDescription;
}
