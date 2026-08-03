package com.vointika.shared.port;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The storefront's read seam onto the tenant: everything the public site needs
 * to render a shop's chrome, addressed by the handle carried in the request's
 * host. Implemented by {@code touroperator}, which owns the row.
 *
 * <p>This is the first port the {@code storefront} context reaches through, and
 * the reason that context is fenced from {@code touroperator} at all — a
 * storefront may never import the admin surface's types.
 *
 * <p><b>Two methods, because a request needs the row in two stages.</b> The
 * password gate and the locale rule both run <em>before</em> a locale exists —
 * they need the operator's own configuration to decide which locale the page is
 * in — while the content needs a locale to overlay its translations against. One
 * method cannot serve both without taking a locale nobody has chosen yet.
 *
 * <p>Every overlay here is <b>nullable-wins-canonical</b>: a translation row has
 * every content column nullable, so a null column falls back to the operator's
 * own value and never to an empty string. A row overlays; it does not replace.
 */
public interface StorefrontShopQuery {

    /**
     * The tenant's gate configuration, addressed by handle. Empty means there is
     * no such storefront.
     */
    Optional<StorefrontGateView> findGate(String handle);

    /**
     * The shop's renderable content in one locale — the caller has already chosen
     * that locale from {@link StorefrontGateView}. An absent translation row is
     * the normal case, not an error.
     */
    Optional<StorefrontShopView> findContent(UUID tourOperatorId, String locale);

    /**
     * @param storefrontPassword the shared gate value, plaintext by design — it is
     *                           something the operator reads back and hands out,
     *                           not a credential — and {@code null} when never set.
     *                           <b>It exists to be compared inside the gate and
     *                           must never reach a view model</b>: a template that
     *                           could read it is a plaintext password on a public
     *                           page.
     * @param passwordMessage    the visitor message, already overlaid from the
     *                           <b>primary-locale</b> translation. The gate runs
     *                           before locale resolution, so the password page has
     *                           no locale of its own to be in.
     * @param supportedLocales   every locale this operator publishes, the primary
     *                           included.
     */
    record StorefrontGateView(
            UUID tourOperatorId,
            boolean passwordEnabled,
            String storefrontPassword,
            String passwordMessage,
            String primaryLocale,
            Set<String> supportedLocales
    ) {}

    /**
     * Media are <b>storage keys, never URLs</b> (PATTERNS §5): the absolute URL
     * is built at read time against the current asset base. An unset — or
     * since-deleted — media reference is absent from the media library lookup
     * and arrives here as {@code null}.
     *
     * <p>Absent SEO fields are {@code null}, never {@code ""}: Mustache's default
     * truthiness treats the empty string as <em>truthy</em>, so a section
     * guarding an optional tag would emit it empty. (The compiler also sets
     * {@code emptyStringIsFalse}; both, belt and braces.)
     */
    record StorefrontShopView(
            String name,
            String logoKey,
            String ogImageKey,
            String seoTitle,
            String seoDescription
    ) {}
}
