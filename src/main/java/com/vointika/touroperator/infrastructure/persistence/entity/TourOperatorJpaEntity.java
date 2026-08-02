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
    private String handle;

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

    /** Storefront password protection (Shopify's Store access model). */
    @Column(nullable = false)
    private boolean passwordEnabled;

    /** Shared storefront gate, plaintext by design — see V7. */
    @Column(length = 100)
    private String storefrontPassword;

    /** The optional "Message for your visitors" shown on the password page. */
    @Column(columnDefinition = "text")
    private String passwordMessage;

    /** Shop-level SEO defaults — the fallback every page type resolves through. */
    @Column(length = 70)
    private String seoTitle;

    @Column(length = 320)
    private String seoDescription;

    /** Bare media id; resolved to a URL at read time (PATTERNS §5), like logoMediaId. */
    @Column
    private UUID ogImageMediaId;

    /** The content locales this operator supports — child table, eager (small set). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(schema = "touroperator", name = "tour_operator_locales",
            joinColumns = @JoinColumn(name = "tour_operator_id"))
    @Column(name = "locale", nullable = false)
    private Set<String> supportedLocales;
}
