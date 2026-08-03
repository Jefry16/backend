package com.vointika.storefront.application.dto.output;

import java.util.List;

/**
 * Which language this page is in, and where its siblings live.
 *
 * <p><b>The current locale is here rather than on {@link ShopData}</b>: it is a
 * property of the request, not of the operator, and the language switcher needs
 * the list beside it anyway. Shopify splits the same two facts across
 * {@code request.locale} and {@code localization.language}; we have no
 * {@code request} object and nothing that would need one — its other members
 * (design mode, origin, page type) have no counterpart here.
 */
public record LocalizationData(String locale, List<LanguageData> languages) {

    /**
     * @param current    true for the one language this page is being rendered in.
     *                   Exactly one entry carries it.
     * @param pathLocale where this language's pages live: {@code null} for the
     *                   primary, which serves bare, and the code itself for a
     *                   published secondary. The rule is
     *                   {@link com.vointika.storefront.application.policy.LocaleResolver}'s,
     *                   so it is answered here rather than re-derived by whoever
     *                   builds the URL.
     */
    public record LanguageData(String code, boolean current, String pathLocale) {}
}
