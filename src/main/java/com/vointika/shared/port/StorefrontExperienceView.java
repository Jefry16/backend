package com.vointika.shared.port;

import java.util.List;
import java.util.Map;

/**
 * A published experience as the public storefront shows it, already resolved for
 * one locale: translated fields overlaid on the canonical ones, media ids turned
 * into URLs, and the slug that addresses it in that locale.
 *
 * <p>Deliberately narrower than the admin's view of an experience. There is no
 * id, no draft state, no audit metadata and no booking cutoff — a storefront
 * addresses experiences by slug, and everything absent here is something the
 * public page has no business rendering.
 *
 * @param slug    the handle for THIS locale — the localized slug when the
 *                operator set one, otherwise the canonical slug. What a URL is
 *                built from.
 * @param canonicalSlug the operator's original handle, addressable in EVERY
 *                      locale — what a locale with no localized slug falls back
 *                      to when a page links its own translations
 * @param handles       locale → localized handle, for the locales that have one.
 *                      A consumer reads {@code handles[locale] ?? canonicalSlug}
 *                      and always gets a URL that resolves. Empty on list rows,
 *                      which link only within the locale being rendered.
 */
public record StorefrontExperienceView(
        String slug,
        String name,
        String description,
        String longDescription,
        List<String> highlights,
        List<String> included,
        List<String> notIncluded,
        List<String> tags,
        String thumbnailUrl,
        List<String> mediaUrls,
        Integer durationMinutes,
        boolean featured,
        String canonicalSlug,
        Map<String, String> handles) {}
