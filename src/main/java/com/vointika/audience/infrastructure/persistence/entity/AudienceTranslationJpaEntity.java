package com.vointika.audience.infrastructure.persistence.entity;

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
@Table(schema = "audience", name = "audience_translations")
@IdClass(AudienceTranslationId.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AudienceTranslationJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID audienceId;

    @Id
    @Column(nullable = false, updatable = false, length = 8)
    private String locale;

    @Column(nullable = false, updatable = false)
    private UUID tourOperatorId;

    @Column
    private String name;
}
