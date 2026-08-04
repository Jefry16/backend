package com.vointika.storefront.presentation.view;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.storefront.application.dto.output.StorefrontPageData;

/**
 * The Mustache context object for the home template — a view model, not a
 * serialized response, which is why it lives in {@code presentation/view} rather
 * than {@code presentation/response}.
 *
 * <p>It is the four global objects and nothing else, because the home page has
 * nothing of its own to render.
 *
 * <p><b>The four components repeat on every page view, and that is accepted.</b>
 * Records cannot extend, and nesting the envelope would put
 * {@code {{envelope.shop.name}}} in every template for no reader's benefit. Four
 * composite names still beat the six loose scalars they replaced. When sections
 * land the render context becomes globals plus a section — likely a map — and
 * that is the second consumer that would justify one.
 *
 * <p><b>Must stay public, and so must anything nested in it.</b> The compiler
 * runs with access coercion off, so {@code Method.invoke} on a package-private
 * class throws {@code IllegalAccessException} even when the accessor itself is
 * public — at render time, not compile time.
 */
public record HomeView(Shop shop, Page page, Routes routes, Localization localization) {

    /**
     * @param pathLocale the locale prefix the request arrived under, or
     *                   {@code null} for the bare root. <b>Not the rendered
     *                   locale</b> — see {@link Routes}.
     */
    public static HomeView from(StorefrontPageData page, String pathLocale,
                                MediaUrlResolver mediaUrlResolver,
                                       String origin, String path) {
        return new HomeView(
                Shop.from(page.shop(), mediaUrlResolver, origin),
                Page.from(page.page(), mediaUrlResolver, path),
                Routes.forPathLocale(pathLocale),
                Localization.from(page.localization(), Routes::root));
    }
}
