package com.vointika.touroperator.domain.entity;

import com.vointika.touroperator.domain.valueobject.BrandShortDescription;
import com.vointika.touroperator.domain.valueobject.BrandSlogan;
import com.vointika.touroperator.domain.valueobject.OperatorSeoDescription;
import com.vointika.touroperator.domain.valueobject.OperatorSeoTitle;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.Objects;
import java.util.UUID;

/**
 * A per-locale overlay on a {@link TourOperator} — the operator's own translatable
 * text. Every field is nullable: a null field falls back to the canonical
 * operator value at render time.
 *
 * <p>{@code name}, {@code handle} and {@code address} are deliberately absent. A
 * brand name is not content, and the handle is the URL.
 *
 * <p>{@code passwordMessage} lives here rather than only on the operator because
 * the gate page is the one page a visitor may see before any content, and it was
 * previously untranslatable.
 *
 * <p>{@code slogan} and {@code shortDescription} are the brand's translatable
 * text. They arrived as columns in V10 with no write path — the brand slice was
 * read-only end to end — which forced the JPA entity to map them
 * {@code insertable/updatable = false} so that an SEO edit could not blank them.
 * They are carried here now, so the columns are writable and every field on this
 * overlay behaves the same way: set it, blank it to fall back, or delete the
 * locale. The canonical values still live on {@code tour_operator_brand} and
 * still have no admin write path of their own.
 */
public record TourOperatorTranslation(
        UUID tourOperatorId,
        LocaleCode locale,
        OperatorSeoTitle seoTitle,
        OperatorSeoDescription seoDescription,
        String passwordMessage,
        BrandSlogan slogan,
        BrandShortDescription shortDescription) {

    public TourOperatorTranslation {
        Objects.requireNonNull(tourOperatorId, "tourOperatorId");
        Objects.requireNonNull(locale, "locale");
    }

    /** A fully-untranslated locale — every field null (the admin editor's form). */
    public static TourOperatorTranslation empty(UUID tourOperatorId, LocaleCode locale) {
        return new TourOperatorTranslation(tourOperatorId, locale, null, null, null, null, null);
    }

    /**
     * Nothing is translated, so this overlay changes nothing — the read falls
     * back to the operator's own values for every field. Storing such a row is
     * indistinguishable from having no row at all, except that it shows up in
     * the translations list as if a locale had been worked on. The upsert
     * deletes instead of writing one; this is the predicate it asks.
     *
     * <p>Add a translatable component to this record and it belongs here too.
     */
    public boolean isEmpty() {
        return seoTitle == null && seoDescription == null && passwordMessage == null
                && slogan == null && shortDescription == null;
    }
}
