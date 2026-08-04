package com.vointika.storefront.presentation.view;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.storefront.application.dto.output.ShopData;

/**
 * {@code {{shop}}} — the operator, as a template sees it. Part of the storefront's
 * published contract: renaming a component here breaks every theme written
 * against it.
 *
 * <p><b>Public, and so is {@link Currency}.</b> The compiler runs with access
 * coercion off, so {@code Method.invoke} on a package-private class throws
 * {@code IllegalAccessException} at render time even when the accessor itself is
 * public — and a record nested in a public record is package-private unless it
 * says otherwise.
 *
 * <p>It carries no storefront password. The gate's plaintext value exists to be
 * compared inside the gate; a template that could read it would be a plaintext
 * password on a public page.
 */
public record Shop(String name, String address, String phone, String email,
                   String logoUrl, String url, Currency currency) {

    public record Currency(String code, String symbol) {}

    /**
     * @param url the absolute origin this request arrived on, so a theme can form
     *            an absolute address for anything {@code routes} names —
     *            {@code {{shop.url}}{{routes.experiences}}}. Mustache concatenates
     *            by juxtaposition, so this needs no filter and no Java string
     *            building.
     */
    public static Shop from(ShopData shop, MediaUrlResolver mediaUrlResolver, String url) {
        return new Shop(
                shop.name(),
                shop.address(),
                shop.phone(),
                shop.email(),
                mediaUrlResolver.toUrl(shop.logoKey()),
                url,
                new Currency(shop.currency().code(), shop.currency().symbol()));
    }
}
