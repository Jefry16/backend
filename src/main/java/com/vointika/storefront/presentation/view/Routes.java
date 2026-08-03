package com.vointika.storefront.presentation.view;

import com.vointika.storefront.application.policy.StorefrontRoutes;

/**
 * {@code {{routes}}} — where the storefront's pages live, under one locale.
 * Shopify's {@code routes} object, and the same reason for existing: a template
 * that hard-codes {@code /experiences} is a template that breaks the day the
 * path moves, and it cannot know the locale prefix at all.
 *
 * <p>It is not new capability, it is extraction. {@code ExperienceListView}
 * already built {@code /{locale}/experiences/{handle}} card by card, and the
 * detail page would have built the same prefix a second time. Built once from
 * the locale the request arrived under, it also gives the logo a home link for
 * free.
 *
 * <p><b>The prefix is the locale in the path, not the locale being rendered.</b>
 * The primary renders at the bare root, so links from it must stay unprefixed —
 * take the rendered locale instead and every link on the shop's own language
 * points at a 404 by construction.
 *
 * <p>Public for the reason {@link Shop} documents.
 */
public record Routes(String root, String experiences) {

    /**
     * @param pathLocale the locale prefix in the URL, or {@code null} for the
     *                   primary, which serves bare
     */
    public static Routes forPathLocale(String pathLocale) {
        String prefix = pathLocale == null ? "" : "/" + pathLocale;
        return new Routes(
                prefix.isEmpty() ? "/" : prefix,
                prefix + StorefrontRoutes.EXPERIENCES);
    }
}
