package com.vointika.rendering.application.dto.output;

import com.vointika.shared.port.StorefrontOperatorView;

import java.util.List;

/**
 * The chrome-only render context: who the tenant is, and which locale the page
 * is actually being rendered in.
 *
 * <p>Every render context carries these two blocks; page-type contexts (the
 * index, an experience, a CMS page) add their content alongside them. This one
 * carries content for no page at all, which is exactly what the password gate
 * needs — the gate must be renderable without computing anything the visitor
 * has not yet unlocked.
 *
 * @param locale     the locale the page is rendered in, after fallback —
 *                   authoritative, and not necessarily the one that was requested
 * @param navigation the operator's menus, resolved for this locale. Chrome, so
 *                   it rides on every page type rather than being fetched per
 *                   page — the one-call-per-render contract leaves nowhere else
 *                   for it to come from.
 * @param seo        the page's resolved SEO block. Top level rather than on the
 *                   content object, because the home page has no content object
 *                   and would otherwise be the one page unable to carry SEO.
 * @param passwordMessage the gate-page copy for this locale, translated where
 *                   the operator has translated it — resolved here so the
 *                   canonical value on {@code shop} is never the one rendered.
 */
public record ShopRenderContext(
        StorefrontOperatorView shop,
        String locale,
        List<NavigationMenu> navigation,
        Seo seo,
        String passwordMessage) {}
