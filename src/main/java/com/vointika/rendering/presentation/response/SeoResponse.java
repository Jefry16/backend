package com.vointika.rendering.presentation.response;

import com.vointika.rendering.application.dto.output.Seo;

/**
 * The {@code seo} block on the wire — fully resolved strings, so a theme renders
 * them without knowing the fallback order.
 *
 * <p>Top level on the envelope rather than nested on the content object: the
 * home page has no content object, and anywhere else it would be the one page
 * type unable to carry SEO at all.
 */
public record SeoResponse(String title, String description, String imageUrl) {

    public static SeoResponse from(Seo seo) {
        return new SeoResponse(seo.title(), seo.description(), seo.imageUrl());
    }
}
