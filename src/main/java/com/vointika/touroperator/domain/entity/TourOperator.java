package com.vointika.touroperator.domain.entity;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.shared.valueobject.Handle;
import com.vointika.touroperator.domain.valueobject.OperatorSeoDescription;
import com.vointika.touroperator.domain.valueobject.OperatorSeoTitle;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;
import com.vointika.touroperator.domain.valueobject.TourOperatorPhone;
import com.vointika.touroperator.domain.valueobject.TourOperatorEmail;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A tour operator — the tenant aggregate. This is the create-slice shape:
 * identity (name, handle), the two reference ids it operates under (timezone,
 * currency), a postal address, and provenance (createdBy + timestamps).
 *
 * <p>Deliberately minimal — status/lifecycle, fee and reply-to are added by
 * their own later slices, when a feature actually reads them (each field earns
 * its place).
 *
 * <p><b>The logo is not here.</b> V10 moved it onto the operator's brand row,
 * because Shopify has no {@code shop.logo} — only {@code shop.brand.logo} — and
 * a second copy on the aggregate would be a value that drifts. The admin write
 * path follows it, through {@code TourOperatorBrandRepository}.
 */
public class TourOperator {

    private final UUID id;
    private TourOperatorName name;
    private final Handle handle;
    private UUID timezoneId;
    private UUID currencyId;
    private TourOperatorAddress address;
    private LocaleCode primaryLocale;
    private Set<LocaleCode> supportedLocales;
    private boolean passwordEnabled;
    private String storefrontPassword;
    private String passwordMessage;
    private OperatorSeoTitle seoTitle;
    private OperatorSeoDescription seoDescription;
    private UUID ogImageMediaId;
    private TourOperatorPhone phone;
    private TourOperatorEmail email;
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
                        Handle handle,
                        UUID timezoneId,
                        UUID currencyId,
                        TourOperatorAddress address,
                        UUID createdBy) {
        this.id = id;
        this.name = name;
        this.handle = handle;
        this.timezoneId = timezoneId;
        this.currencyId = currencyId;
        this.address = address;
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
                        Handle handle,
                        UUID timezoneId,
                        UUID currencyId,
                        TourOperatorAddress address,
                        UUID createdBy,
                        Instant createdAt,
                        Instant updatedAt,
                        LocaleCode primaryLocale,
                        Set<LocaleCode> supportedLocales) {
        this(id, name, handle, timezoneId, currencyId, address, createdBy, createdAt, updatedAt,
                primaryLocale, supportedLocales, false, null, null, null, null, null, null, null);
    }

    // Constructor for reconstituting from persistence
    public TourOperator(UUID id,
                        TourOperatorName name,
                        Handle handle,
                        UUID timezoneId,
                        UUID currencyId,
                        TourOperatorAddress address,
                        UUID createdBy,
                        Instant createdAt,
                        Instant updatedAt,
                        LocaleCode primaryLocale,
                        Set<LocaleCode> supportedLocales,
                        boolean passwordEnabled,
                        String storefrontPassword,
                        String passwordMessage,
                        OperatorSeoTitle seoTitle,
                        OperatorSeoDescription seoDescription,
                        UUID ogImageMediaId,
                        TourOperatorPhone phone,
                        TourOperatorEmail email) {
        this.id = id;
        this.name = name;
        this.handle = handle;
        this.timezoneId = timezoneId;
        this.currencyId = currencyId;
        this.address = address;
        this.primaryLocale = primaryLocale;
        this.supportedLocales = new LinkedHashSet<>(supportedLocales);
        this.passwordEnabled = passwordEnabled;
        this.storefrontPassword = storefrontPassword;
        this.passwordMessage = passwordMessage;
        this.seoTitle = seoTitle;
        this.seoDescription = seoDescription;
        this.ogImageMediaId = ogImageMediaId;
        this.phone = phone;
        this.email = email;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    /**
     * Replaces the shop-level SEO defaults. Each argument is the whole new value:
     * null clears the override, which is what the storefront reads as "fall back"
     * — an empty string would be a title of zero characters, not the absence of
     * one.
     */
    public void updateSeo(OperatorSeoTitle newSeoTitle,
                          OperatorSeoDescription newSeoDescription,
                          UUID newOgImageMediaId) {
        this.seoTitle = newSeoTitle;
        this.seoDescription = newSeoDescription;
        this.ogImageMediaId = newOgImageMediaId;
        this.updatedAt = Instant.now();
    }

    /**
     * Replaces the operator's own details. Every argument is the <b>new value or
     * null to clear</b> — the use case has already decided which fields the
     * PATCH touched, because a record cannot tell an absent JSON field from an
     * explicit null.
     *
     * <p>{@code handle} is not here and never will be: it is the storefront
     * subdomain, so changing it moves the shop's address.
     *
     * <p><b>Changing {@code timezoneId} reinterprets every stored departure.</b>
     * Slots hold operator-LOCAL wall-clock times ({@code LocalDateTime}, no
     * zone), so a 10:00 sailing stays "10:00" and silently becomes a different
     * instant — and the past-date guard on slot creation starts judging against
     * a different today. Nothing here rewrites those rows. This is allowed by
     * decision, not by oversight, and the audit entry records the change so the
     * reinterpretation is at least traceable.
     */
    public void updateDetails(TourOperatorName newName,
                              TourOperatorAddress newAddress,
                              TourOperatorPhone newPhone,
                              TourOperatorEmail newEmail,
                              UUID newTimezoneId,
                              UUID newCurrencyId) {
        this.name = newName;
        this.address = newAddress;
        this.phone = newPhone;
        this.email = newEmail;
        this.timezoneId = newTimezoneId;
        this.currencyId = newCurrencyId;
        this.updatedAt = Instant.now();
    }

    /** Audit-worthy, non-sensitive fields — never the storefront password. */
    public Map<String, Object> auditSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("name", name == null ? null : name.value());
        // Six flat keys, not one composed line: AuditChanges.diff is a flat map
        // diff, so this reads "city: Madrid -> Palma" in the activity log instead
        // of one opaque blob that changes wholesale. countryId goes in as a UUID
        // string, exactly as timezoneId and currencyId already do.
        snapshot.put("address1", address == null ? null : address.address1());
        snapshot.put("address2", address == null ? null : address.address2());
        snapshot.put("city", address == null ? null : address.city());
        snapshot.put("province", address == null ? null : address.province());
        snapshot.put("zip", address == null ? null : address.zip());
        snapshot.put("countryId",
                address == null || address.countryId() == null ? null : address.countryId().toString());
        snapshot.put("phone", phone == null ? null : phone.value());
        snapshot.put("email", email == null ? null : email.value());
        snapshot.put("timezoneId", timezoneId == null ? null : timezoneId.toString());
        snapshot.put("currencyId", currencyId == null ? null : currencyId.toString());
        return snapshot;
    }

    public UUID getId() { return id; }
    public TourOperatorName getName() { return name; }
    public Handle getHandle() { return handle; }
    public UUID getTimezoneId() { return timezoneId; }
    public UUID getCurrencyId() { return currencyId; }
    public TourOperatorAddress getAddress() { return address; }
    public LocaleCode getPrimaryLocale() { return primaryLocale; }
    public Set<LocaleCode> getSupportedLocales() { return Set.copyOf(supportedLocales); }
    public boolean isPasswordEnabled() { return passwordEnabled; }
    public String getStorefrontPassword() { return storefrontPassword; }
    public String getPasswordMessage() { return passwordMessage; }
    public OperatorSeoTitle getSeoTitle() { return seoTitle; }
    public OperatorSeoDescription getSeoDescription() { return seoDescription; }
    public UUID getOgImageMediaId() { return ogImageMediaId; }
    public TourOperatorPhone getPhone() { return phone; }
    public TourOperatorEmail getEmail() { return email; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
