package com.vointika.rendering.application.service;

import com.vointika.shared.port.StorefrontOperatorView;

import java.util.Locale;

/**
 * Resolves which locale a storefront page is rendered in.
 *
 * <p>The rule is deliberately <em>lenient</em> here: an unknown or unpublished
 * request falls back to the operator's primary locale rather than failing, and
 * the resolved locale is returned to the caller as authoritative.
 *
 * <p>The <em>strict</em> half of the rule lives in the storefront BFF, which
 * owns URLs: Shopify serves the primary locale on the bare path and gives a URL
 * prefix only to published secondary locales, so {@code /en/…} when English is
 * primary — and any unpublished prefix — is a 404, never a silent fallback.
 * The BFF has the operator's locale list from the {@code shop} block and makes
 * that call before it ever asks for content. This backend rule is what catches
 * everything reaching it by another route (a query parameter, an internal
 * re-render) without turning a bad locale into a broken page.
 */
public final class LocaleResolver {

    private LocaleResolver() {}

    /** The requested locale if the operator publishes it, else its primary. */
    public static String resolve(StorefrontOperatorView operator, String requested) {
        if (requested == null) {
            return operator.primaryLocale();
        }
        // Locale.ROOT, never the JVM default — matching LocaleCode. Under a
        // Turkish default locale "IT".toLowerCase() is "ıt" (dotless), so an
        // Italian storefront would silently fall back to its primary language
        // depending on which machine served the request.
        String normalized = requested.trim().toLowerCase(Locale.ROOT);
        return operator.supportedLocales().contains(normalized)
                ? normalized
                : operator.primaryLocale();
    }
}
