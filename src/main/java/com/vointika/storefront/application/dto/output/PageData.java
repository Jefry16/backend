package com.vointika.storefront.application.dto.output;

/**
 * What is true of <em>this</em> page rather than of the shop: the title in the
 * tab, the meta description, the social image.
 *
 * <p><b>The split from {@link ShopData} is the point.</b> Both the home page and
 * the listing currently take their title from the shop's SEO title falling back
 * to its name, which reads as if title were a property of the shop; it is not,
 * and a page type with SEO text of its own (an experience, a CMS page) sets it
 * without touching the shop. Keeping them in one object is what would make
 * {@code page.title} meaningless on page three.
 */
public record PageData(
        String title,
        String description,
        String ogImageKey
) {}
