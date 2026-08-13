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

    // The postal address, in parts (V15). All nullable: dropping the old
    // free-text column left every existing row without one, and a half-written
    // address is refused by a CHECK rather than by these annotations.
    @Column(length = 255)
    private String address1;

    @Column(length = 255)
    private String address2;

    @Column(length = 120)
    private String city;

    @Column(length = 120)
    private String province;

    @Column(length = 20)
    private String zip;

    @Column
    private UUID countryId;

    /**
     * The shop's public contact details (V9), <b>mapped read-only on purpose</b>.
     *
     * <p>There is no write path yet, and the domain {@code TourOperator} does not
     * carry them — so {@code TourOperatorMapper.toJpa} rebuilds this entity from a
     * domain object that has nothing to put here. Left writable, that means every
     * unrelated operator save (a logo, a locale set, an SEO edit) would issue an
     * UPDATE nulling both columns. {@code insertable/updatable = false} keeps them
     * out of every INSERT and UPDATE Hibernate generates, so a read-only column
     * cannot be clobbered by a write that never meant to touch it. The slice that
     * adds the write path flips these two flags, deliberately.
     */
    @Column(length = 30)
    private String phone;

    @Column(length = 320)
    private String email;

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
