package com.vointika.media.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "media", name = "media")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MediaJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID tourOperatorId;

    @Column(nullable = false, updatable = false, unique = true)
    private String storageKey;

    @Column(nullable = false, updatable = false)
    private String contentType;

    @Column(nullable = false, updatable = false)
    private long sizeBytes;

    @Column(nullable = false, updatable = false)
    private String originalName;

    /**
     * What a rendered {@code <img>} needs beyond its {@code src} — the shape
     * Shopify's image object carries. <b>Mapped read-only, and for the reason
     * {@code TourOperatorJpaEntity}'s phone and email are</b> (#94): the domain
     * {@code Media} carries none of them, so {@code MediaMapper} rebuilds this
     * entity with nulls and a writable column would be wiped by any unrelated
     * save. The slice that adds a write path flips these flags.
     *
     * <p>{@code alt} cannot be derived — only the uploader knows it, so it needs
     * an admin field. {@code width}/{@code height} must be measured at upload,
     * which needs a port: {@code javax.imageio} is not {@code java.*}, and the
     * application layer's allowlist has no exemptions (the trap #91 hit with
     * {@code javax.crypto}). A theme derives the aspect ratio from the pair.
     */
    @Column(length = 500, insertable = false, updatable = false)
    private String alt;

    @Column(insertable = false, updatable = false)
    private Integer width;

    @Column(insertable = false, updatable = false)
    private Integer height;

    @Column(nullable = false, updatable = false)
    private UUID createdBy;

    @Column(nullable = false, updatable = false)
    private String createdByName;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
