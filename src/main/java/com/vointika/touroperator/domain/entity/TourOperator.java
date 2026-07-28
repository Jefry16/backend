package com.vointika.touroperator.domain.entity;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.shared.valueobject.Slug;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
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
    private LocaleCode primaryLocale;
    private Set<LocaleCode> supportedLocales;
    private boolean passwordEnabled;
    private String storefrontPassword;
    private String passwordMessage;
    private final UUID createdBy;
    private final Instant createdAt;
    private Instant updatedAt;

    private static final int PASSWORD_MAX_LENGTH = 100;
    private static final int PASSWORD_MESSAGE_MAX_LENGTH = 1000;

    /** The locale a brand-new operator gets until it configures its own. */
    private static final LocaleCode DEFAULT_LOCALE = LocaleCode.of("en");

    // Constructor for creating a brand new tour operator (no logo; default locale)
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
        this.primaryLocale = DEFAULT_LOCALE;
        this.supportedLocales = new LinkedHashSet<>(Set.of(DEFAULT_LOCALE));
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Convenience reconstitution without the password-protection slice
    // (defaults: protection off). Kept for tests; the mapper uses the full
    // constructor below.
    public TourOperator(UUID id,
                        TourOperatorName name,
                        Slug slug,
                        UUID timezoneId,
                        UUID currencyId,
                        TourOperatorAddress address,
                        UUID createdBy,
                        Instant createdAt,
                        Instant updatedAt,
                        UUID logoMediaId,
                        LocaleCode primaryLocale,
                        Set<LocaleCode> supportedLocales) {
        this(id, name, slug, timezoneId, currencyId, address, createdBy, createdAt, updatedAt,
                logoMediaId, primaryLocale, supportedLocales, false, null, null);
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
                        UUID logoMediaId,
                        LocaleCode primaryLocale,
                        Set<LocaleCode> supportedLocales,
                        boolean passwordEnabled,
                        String storefrontPassword,
                        String passwordMessage) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.timezoneId = timezoneId;
        this.currencyId = currencyId;
        this.address = address;
        this.logoMediaId = logoMediaId;
        this.primaryLocale = primaryLocale;
        this.supportedLocales = new LinkedHashSet<>(supportedLocales);
        this.passwordEnabled = passwordEnabled;
        this.storefrontPassword = storefrontPassword;
        this.passwordMessage = passwordMessage;
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

    /**
     * Replaces the operator's content languages. {@code supported} must be
     * non-empty and {@code primary} must be one of them (else 422). Codes are
     * validated against the master list by the caller before this runs.
     */
    public void updateLocales(LocaleCode primary, Set<LocaleCode> supported) {
        if (supported == null || supported.isEmpty()) {
            throw new InvalidFieldException("At least one supported locale is required");
        }
        if (primary == null || !supported.contains(primary)) {
            throw new InvalidFieldException("The primary locale must be one of the supported locales");
        }
        this.primaryLocale = primary;
        this.supportedLocales = new LinkedHashSet<>(supported);
        this.updatedAt = Instant.now();
    }

    /**
     * Updates storefront password protection (Shopify's Store access model).
     * A non-blank {@code password} replaces the stored one; null/blank keeps
     * it (the form can flip the toggle or edit the message without re-sending
     * the password). Enabling with no password stored and none provided is a
     * 422. A null/blank {@code message} clears it. Disabling keeps password +
     * message so re-enabling restores them.
     */
    public void updateStorefrontPassword(boolean enabled, String password, String message) {
        if (password != null && !password.isBlank()) {
            String trimmed = password.trim();
            if (trimmed.length() > PASSWORD_MAX_LENGTH) {
                throw new InvalidFieldException(
                        "Storefront password must be at most " + PASSWORD_MAX_LENGTH + " characters");
            }
            this.storefrontPassword = trimmed;
        }
        if (enabled && (this.storefrontPassword == null || this.storefrontPassword.isBlank())) {
            throw new InvalidFieldException("A password is required to enable password protection");
        }
        if (message == null || message.isBlank()) {
            this.passwordMessage = null;
        } else {
            String trimmedMessage = message.trim();
            if (trimmedMessage.length() > PASSWORD_MESSAGE_MAX_LENGTH) {
                throw new InvalidFieldException("Password page message must be at most "
                        + PASSWORD_MESSAGE_MAX_LENGTH + " characters");
            }
            this.passwordMessage = trimmedMessage;
        }
        this.passwordEnabled = enabled;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public TourOperatorName getName() { return name; }
    public Slug getSlug() { return slug; }
    public UUID getTimezoneId() { return timezoneId; }
    public UUID getCurrencyId() { return currencyId; }
    public TourOperatorAddress getAddress() { return address; }
    public UUID getLogoMediaId() { return logoMediaId; }
    public LocaleCode getPrimaryLocale() { return primaryLocale; }
    public Set<LocaleCode> getSupportedLocales() { return Set.copyOf(supportedLocales); }
    public boolean isPasswordEnabled() { return passwordEnabled; }
    public String getStorefrontPassword() { return storefrontPassword; }
    public String getPasswordMessage() { return passwordMessage; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
