package com.vointika.rendering.application.service;

import com.vointika.rendering.application.dto.output.Seo;
import com.vointika.shared.port.StorefrontOperatorTranslationView;
import com.vointika.shared.port.StorefrontOperatorView;

/**
 * Resolves a page's SEO block by walking its fallback chain.
 *
 * <p><b>Two stages, kept apart on purpose.</b> A content context resolves its
 * own chain and may return null — {@code experience} must not learn that
 * operators have SEO defaults. This class holds the second stage: it fills
 * whatever the content left null from the shop, and it is the only place that
 * sees both.
 *
 * <p>Every shop-level term reads the <em>translated</em> operator value before
 * the canonical one, which is the whole reason the operator has a translation
 * table.
 *
 * <p>The shop name is deliberately never appended to a title. "X — Acme Tours"
 * is a presentation pattern, and the consumer already has {@code shop.name}.
 */
public final class SeoResolver {

    private SeoResolver() {}

    /**
     * The home page's block. Home has no content object, so every term is
     * shop-level: title falls back to the shop's name, which is the only title
     * source it has, and the image to the logo.
     */
    public static Seo forHome(StorefrontOperatorView shop, String locale) {
        StorefrontOperatorTranslationView overlay = shop.translation(locale);
        return new Seo(
                firstNonBlank(overlay.seoTitle(), shop.seoTitle(), shop.name()),
                firstNonBlank(overlay.seoDescription(), shop.seoDescription()),
                firstNonBlank(shop.ogImageUrl(), shop.logoUrl()));
    }

    /** The shop's message on the gate page, translated where the operator has. */
    public static String passwordMessage(StorefrontOperatorView shop, String locale) {
        return firstNonBlank(shop.translation(locale).passwordMessage(), shop.passwordMessage());
    }

    /** First value that is neither null nor blank, or null when none is. */
    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }
}
