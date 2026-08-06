package com.vointika.shared.port;

import java.util.List;
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
 * <p><b>Three methods, and the first two are two because a request needs the row
 * in two stages.</b> The password gate and the locale rule both run
 * <em>before</em> a locale exists — they need the operator's own configuration
 * to decide which locale the page is in — while the content needs a locale to
 * overlay its translations against. One method cannot serve both without taking
 * a locale nobody has chosen yet.
 *
 * <p>{@link #findPolicy} is the third for a different reason: <b>two callers
 * want different depths of the same rows</b>. Every page's footer needs the
 * links and only the links, while one route needs a body — and carrying four
 * HTML documents on every render to produce four {@code <a>} tags is the wrong
 * trade.
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
     * One legal document, with its body, in the locale the caller resolved.
     *
     * <p>Empty covers both misses the storefront has to answer 404 for, and
     * deliberately does not distinguish them: <b>a {@code type} no policy type is
     * named after</b> — the value comes from a path segment, so
     * {@code /policies/refunds} arrives here — and a type the operator has not
     * written. There is no draft state; the row's absence is the unpublished one.
     *
     * @param type a {@code PolicyType} name, a primitive because the enum belongs
     *             to the context that owns the row (PATTERNS §6). An unknown name
     *             is empty, never an exception.
     */
    Optional<StorefrontPolicyView> findPolicy(UUID tourOperatorId, String type, String locale);

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
     * <p><b>There is no {@code logoKey}.</b> The logo moved onto
     * {@link StorefrontBrandView} with V10, because Shopify's shop object has no
     * logo of its own — only {@code shop.brand.logo}.
     *
     * <p>Absent SEO fields are {@code null}, never {@code ""}: Mustache's default
     * truthiness treats the empty string as <em>truthy</em>, so a section
     * guarding an optional tag would emit it empty. (The compiler also sets
     * {@code emptyStringIsFalse}; both, belt and braces.)
     *
     * @param address        the operator's own address, a NOT NULL column and
     *                       deliberately <b>not</b> translated — one street
     *                       exists in one language.
     * @param phone          public contact details, both nullable and both
     *                       untranslated for the same reason as {@code address}.
     *                       <b>Null on every row today</b>: V9 added the columns
     *                       and no write path sets them yet.
     * @param currencyCode   the operator's currency, joined from
     *                       {@code reference.currencies}. {@code reference} is a
     *                       shared kernel, so this is a join inside
     *                       {@code touroperator}'s adapter rather than a second
     *                       port. Both halves are {@code null} only if the
     *                       reference row has gone, which the FK forbids.
     * @param policies       the operator's legal documents <b>without their
     *                       bodies</b> — every page's footer links them and only
     *                       the policy route reads one. Ordered by type, and
     *                       empty for an operator who has written none.
     */
    record StorefrontShopView(
            String name,
            String address,
            String phone,
            String email,
            String ogImageKey,
            String currencyCode,
            String currencySymbol,
            String timezoneName,
            String timezoneCity,
            String seoTitle,
            String seoDescription,
            String passwordMessage,
            StorefrontBrandView brand,
            List<StorefrontPolicySummaryView> policies
    ) {}

    /**
     * A policy as the footer needs it. Both text fields overlay per locale, so a
     * title translated into Spanish arrives Spanish here and an untranslated one
     * arrives canonical.
     *
     * @param type a {@code PolicyType} name — the enum stays in the context that
     *             owns it, and the caller turns the name into an address
     */
    record StorefrontPolicySummaryView(String type, String title) {}

    /**
     * A policy as its own page needs it.
     *
     * @param body <b>raw HTML the operator wrote</b>, stored verbatim and handed
     *             over unescaped. The storefront renders it with Mustache's
     *             unescaped tag on purpose — escaping it would put
     *             {@code &lt;p&gt;} on the page — and the trust boundary that
     *             makes that correct is that this is <em>our</em> template
     *             rendering <em>their</em> content, which is not the same
     *             question as running a template they wrote.
     */
    record StorefrontPolicyView(String type, String title, String body) {}

    /**
     * The operator's brand — Shopify's {@code shop.brand}, which is where the
     * logo lives: there is no {@code shop.logo} there and there is none here.
     *
     * <p><b>Never null, even for an operator with no brand row.</b> An absent row
     * arrives as this record with null fields and empty lists, so a page renders
     * without a brand rather than failing on one — and a theme's
     * {@code {{#shop.brand.logo}}} means the same thing either way.
     *
     * <p>The images are {@link MediaAssetBatchQuery.MediaAsset}s rather than bare keys because an
     * {@code <img>} needs more than a {@code src}. All four resolve through one
     * batch call with the OG image.
     *
     * @param socialLinks ordered by platform, because a set has no order of its
     *                    own and a list that reorders between requests reads as a
     *                    bug
     */
    record StorefrontBrandView(
            String slogan,
            String shortDescription,
            MediaAssetBatchQuery.MediaAsset logo,
            MediaAssetBatchQuery.MediaAsset squareLogo,
            MediaAssetBatchQuery.MediaAsset favicon,
            MediaAssetBatchQuery.MediaAsset coverImage,
            StorefrontBrandColorsView colors,
            List<StorefrontBrandSocialLinkView> socialLinks
    ) {}

    /**
     * The palette, split by role and ordered by position within each — which is
     * what makes {@code colors.primary[0]} a promise rather than a coincidence.
     *
     * <p><b>The split is the owning context's, not the caller's.</b> A role is a
     * {@code touroperator} enum and may not cross this seam (PATTERNS §6); handing
     * over a flat list tagged with a role string would force the caller to compare
     * against literals, i.e. to keep a second copy of an enum it cannot see.
     * Both lists are present and may be empty.
     */
    record StorefrontBrandColorsView(
            List<StorefrontBrandColorView> primary,
            List<StorefrontBrandColorView> secondary
    ) {}

    /** One palette entry: what to paint behind, and what reads on top of it. */
    record StorefrontBrandColorView(String background, String foreground) {}

    /** @param platform a {@code BrandSocialPlatform} name, a primitive for the reason above. */
    record StorefrontBrandSocialLinkView(String platform, String url) {}
}
