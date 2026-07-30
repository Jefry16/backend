package com.vointika.rendering.presentation.response;

import com.vointika.shared.port.StorefrontPageView;

import java.util.Map;

/**
 * A CMS page on the wire, resolved for one locale.
 *
 * <p>{@code body} is raw operator-authored HTML. The renderer escapes by default,
 * so a theme must mark this one value {@code | raw} — deliberately the only such
 * value on the storefront, and the reason page bodies are stored verbatim.
 */
public record PageResponse(
        String handle,
        String title,
        String body,
        String seoTitle,
        String seoDescription,
        String templateSuffix,
        String canonicalHandle,
        Map<String, String> handles) {

    public static PageResponse from(StorefrontPageView page) {
        return new PageResponse(
                page.handle(),
                page.title(),
                page.body(),
                page.seoTitle(),
                page.seoDescription(),
                page.templateSuffix(),
                page.canonicalHandle(),
                page.handles());
    }
}
