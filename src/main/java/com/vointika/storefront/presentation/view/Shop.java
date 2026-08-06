package com.vointika.storefront.presentation.view;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.storefront.application.dto.output.PolicyData;
import com.vointika.storefront.application.dto.output.ShopData;

import java.util.List;

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
 * <p><b>There is no {@code logoUrl}.</b> The logo is {@code shop.brand.logo},
 * where Shopify keeps it — their shop object has no logo of its own. Removing it
 * is a breaking change to a published contract, and it was made while no
 * operator theme exists to break.
 *
 * <p>It carries no storefront password. The gate's plaintext value exists to be
 * compared inside the gate; a template that could read it would be a plaintext
 * password on a public page.
 */
public record Shop(String id, String name, String address, String phone, String email,
                   String url, String description, String passwordMessage,
                   Brand brand, List<Policy> policies,
                   Policy cancellationPolicy, Policy privacyPolicy,
                   Policy termsOfService, Policy legalNotice,
                   Currency currency, Timezone timezone) {

    public record Currency(String code, String symbol) {}

    /** The IANA zone and its city — {@code Europe/Madrid}, {@code Madrid}. */
    public record Timezone(String name, String city) {}

    /**
     * One legal document as every page links it: <b>no body</b>, which only the
     * policy's own page reads.
     *
     * @param url this policy's address in the locale being rendered
     */
    public record Policy(String type, String title, String url) {}

    /**
     * @param url the absolute origin this request arrived on, so a theme can form
     *            an absolute address for anything {@code routes} names —
     *            {@code {{shop.url}}{{routes.experiences}}}. Mustache concatenates
     *            by juxtaposition, so this needs no filter and no Java string
     *            building.
     * @param routes needed for the policies' addresses, because a policy's URL
     *            carries the locale prefix and that rule lives in {@link Routes}
     */
    public static Shop from(ShopData shop, MediaUrlResolver mediaUrlResolver, String url, Routes routes) {
        List<Policy> policies = shop.policies().stream()
                .map(policy -> toPolicy(policy, routes))
                .toList();
        return new Shop(
                shop.id().toString(),
                shop.name(),
                shop.address(),
                shop.phone(),
                shop.email(),
                url,
                shop.description(),
                shop.passwordMessage(),
                Brand.from(shop.brand(), mediaUrlResolver),
                policies,
                named(policies, "CANCELLATION"),
                named(policies, "PRIVACY"),
                named(policies, "TERMS"),
                named(policies, "LEGAL_NOTICE"),
                new Currency(shop.currency().code(), shop.currency().symbol()),
                new Timezone(shop.timezone().name(), shop.timezone().city()));
    }

    private static Policy toPolicy(PolicyData policy, Routes routes) {
        return new Policy(policy.type(), policy.title(), routes.policy(policy.slug()));
    }

    /**
     * Shopify's named access — {@code shop.refund_policy} and its siblings —
     * beside the list, because a theme that wants the cancellation terms in the
     * booking form should not have to iterate and compare.
     *
     * <p>The four names are <b>not</b> derived from the type: {@code TERMS} is
     * {@code termsOfService} because that is what a theme author coming from
     * Shopify types. Null when the operator has not written that one, so a
     * template guards on the object.
     */
    private static Policy named(List<Policy> policies, String type) {
        return policies.stream().filter(policy -> policy.type().equals(type)).findFirst().orElse(null);
    }
}
