package com.vointika.storefront.presentation.view;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.storefront.application.dto.output.HomePageOutput;

/**
 * The Mustache context object for the home template — a view model, not a
 * serialized response, which is why it lives in {@code presentation/view} rather
 * than {@code presentation/response}.
 *
 * <p><b>Must stay public, and so must anything nested in it.</b> The compiler
 * runs with access coercion off, so {@code Method.invoke} on a package-private
 * class throws {@code IllegalAccessException} even when the accessor itself is
 * public — at render time, not compile time.
 */
public record HomeView(
        String title,
        String shopName,
        String description,
        String logoUrl,
        String ogImageUrl
) {

    public static HomeView from(HomePageOutput page, MediaUrlResolver mediaUrlResolver) {
        return new HomeView(
                page.title(),
                page.shopName(),
                page.description(),
                mediaUrlResolver.toUrl(page.logoKey()),
                mediaUrlResolver.toUrl(page.ogImageKey()));
    }
}
