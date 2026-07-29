package com.vointika.rendering.application.dto.output;

import com.vointika.shared.port.StorefrontOperatorView;

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
 * @param locale the locale the page is rendered in, after fallback —
 *               authoritative, and not necessarily the one that was requested
 */
public record ShopRenderContext(StorefrontOperatorView shop, String locale) {}
