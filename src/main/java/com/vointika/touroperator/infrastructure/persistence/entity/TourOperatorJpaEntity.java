package com.vointika.touroperator.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(schema = "touroperator", name = "tour_operators")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TourOperatorJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private UUID timezoneId;

    @Column(nullable = false)
    private UUID currencyId;

    @Column(nullable = false)
    private String address;

    /** Nullable reference to one of this operator's media records (bare id, no FK). */
    @Column
    private UUID logoMediaId;

    @Column(nullable = false, updatable = false)
    private UUID createdBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    /** The operator's default content locale (a code from reference.languages). */
    @Column(nullable = false)
    private String primaryLocale;

    /** The content locales this operator supports — child table, eager (small set). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(schema = "touroperator", name = "tour_operator_locales",
            joinColumns = @JoinColumn(name = "tour_operator_id"))
    @Column(name = "locale", nullable = false)
    private Set<String> supportedLocales;
}
