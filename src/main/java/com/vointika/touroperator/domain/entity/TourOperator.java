package com.vointika.touroperator.domain.entity;

import com.vointika.touroperator.domain.valueobject.Slug;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;

import java.time.Instant;
import java.util.UUID;

/**
 * A tour operator — the tenant aggregate. This is the create-slice shape:
 * identity (name, slug), the two reference ids it operates under (timezone,
 * currency), a postal address, and provenance (createdBy + timestamps).
 *
 * <p>Deliberately minimal — status/lifecycle, logo, fee, branding, locales and
 * reply-to are added by their own later slices, when a feature actually reads
 * them (each field earns its place).
 */
public class TourOperator {

    private final UUID id;
    private TourOperatorName name;
    private final Slug slug;
    private UUID timezoneId;
    private UUID currencyId;
    private TourOperatorAddress address;
    private UUID logoMediaId;
    private final UUID createdBy;
    private final Instant createdAt;
    private Instant updatedAt;

    // Constructor for creating a brand new tour operator (no logo yet)
    public TourOperator(UUID id,
                        TourOperatorName name,
                        Slug slug,
                        UUID timezoneId,
                        UUID currencyId,
                        TourOperatorAddress address,
                        UUID createdBy) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.timezoneId = timezoneId;
        this.currencyId = currencyId;
        this.address = address;
        this.logoMediaId = null;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Constructor for reconstituting from persistence
    public TourOperator(UUID id,
                        TourOperatorName name,
                        Slug slug,
                        UUID timezoneId,
                        UUID currencyId,
                        TourOperatorAddress address,
                        UUID createdBy,
                        Instant createdAt,
                        Instant updatedAt,
                        UUID logoMediaId) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.timezoneId = timezoneId;
        this.currencyId = currencyId;
        this.address = address;
        this.logoMediaId = logoMediaId;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Points the operator's logo at one of its media records (validated by the caller). */
    public void setLogo(UUID mediaId) {
        this.logoMediaId = mediaId;
        this.updatedAt = Instant.now();
    }

    /** Removes the operator's logo. */
    public void clearLogo() {
        this.logoMediaId = null;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public TourOperatorName getName() { return name; }
    public Slug getSlug() { return slug; }
    public UUID getTimezoneId() { return timezoneId; }
    public UUID getCurrencyId() { return currencyId; }
    public TourOperatorAddress getAddress() { return address; }
    public UUID getLogoMediaId() { return logoMediaId; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
