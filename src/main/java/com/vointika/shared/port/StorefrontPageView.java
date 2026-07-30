package com.vointika.shared.port;

import java.util.Map;

/**
 * A published CMS page as the public storefront shows it, already resolved for
 * one locale.
 *
 * @param handle   the handle for THIS locale — the localized one when the
 *                 operator set it, otherwise the canonical handle
 * @param body     raw HTML, stored verbatim by the operator. Escaping is the
 *                 renderer's call, and the renderer escapes by default — this is
 *                 the value a theme must explicitly mark {@code | raw}.
 * @param canonicalHandle the operator's original handle, addressable in EVERY
 *                        locale — the fallback when a locale has no localized one
 * @param handles         locale → localized handle, for the locales that have
 *                        one. Read {@code handles[locale] ?? canonicalHandle}.
 */
public record StorefrontPageView(
        String handle,
        String title,
        String body,
        String seoTitle,
        String seoDescription,
        String templateSuffix,
        String canonicalHandle,
        Map<String, String> handles) {}
