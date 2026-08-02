package com.vointika.shared.port;

/**
 * One locale's overlay on the shop's own text. Every field is nullable — a null
 * field means "not translated", and the reader falls back to the canonical
 * operator value.
 *
 * <p>These ride along on {@link StorefrontOperatorView} rather than being
 * fetched per locale, because the locale is resolved <em>from</em> the operator:
 * {@code LocaleResolver} needs the primary and supported locales before it can
 * choose, so the operator is already loaded by the time the locale is known.
 * Taking a locale on {@code findBySlug} would force either two round trips or
 * locale resolution moving into {@code touroperator}, which does not own it. An
 * operator has a handful of locales, so carrying them all is one query.
 */
public record StorefrontOperatorTranslationView(
        String seoTitle,
        String seoDescription,
        String passwordMessage) {}
