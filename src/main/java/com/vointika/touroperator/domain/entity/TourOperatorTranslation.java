package com.vointika.touroperator.domain.entity;

import com.vointika.touroperator.domain.valueobject.OperatorSeoDescription;
import com.vointika.touroperator.domain.valueobject.OperatorSeoTitle;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.Objects;
import java.util.UUID;

/**
 * A per-locale overlay on a {@link TourOperator} — the shop's own translatable
 * text. Every field is nullable: a null field falls back to the canonical
 * operator value at render time.
 *
 * <p>{@code name}, {@code slug} and {@code address} are deliberately absent. A
 * brand name is not content, and the slug is the URL.
 *
 * <p>{@code passwordMessage} lives here rather than only on the operator because
 * the gate page is the one page a visitor may see before any content, and it was
 * previously untranslatable.
 */
public record TourOperatorTranslation(
        UUID tourOperatorId,
        LocaleCode locale,
        OperatorSeoTitle seoTitle,
        OperatorSeoDescription seoDescription,
        String passwordMessage) {

    public TourOperatorTranslation {
        Objects.requireNonNull(tourOperatorId, "tourOperatorId");
        Objects.requireNonNull(locale, "locale");
    }

    /** A fully-untranslated locale — every field null (the admin editor's form). */
    public static TourOperatorTranslation empty(UUID tourOperatorId, LocaleCode locale) {
        return new TourOperatorTranslation(tourOperatorId, locale, null, null, null);
    }
}
