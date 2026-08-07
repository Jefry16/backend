package com.vointika.touroperator.domain.entity;

import com.vointika.touroperator.domain.enums.PolicyType;
import com.vointika.touroperator.domain.valueobject.PolicyBody;
import com.vointika.touroperator.domain.valueobject.PolicyTitle;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.Objects;
import java.util.UUID;

/**
 * A per-locale overlay on one {@link Policy} — the fourth table to use the
 * translation-overlay shape (PATTERNS §4e), and the first keyed on a composite
 * owner rather than a single id.
 *
 * <p>Both content fields are nullable and the read overlays
 * <b>nullable-wins-canonical</b>: a translated title over an untranslated body
 * renders the Spanish heading above the English document, which is the realistic
 * partial-translation case rather than a defect. A row overlays; it never
 * replaces.
 */
public record PolicyTranslation(
        UUID tourOperatorId,
        PolicyType type,
        LocaleCode locale,
        PolicyTitle title,
        PolicyBody body) {

    public PolicyTranslation {
        Objects.requireNonNull(tourOperatorId, "tourOperatorId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(locale, "locale");
    }

    /** A fully-untranslated locale — both fields null (the admin editor's form). */
    public static PolicyTranslation empty(UUID tourOperatorId, PolicyType type, LocaleCode locale) {
        return new PolicyTranslation(tourOperatorId, type, locale, null, null);
    }
}
