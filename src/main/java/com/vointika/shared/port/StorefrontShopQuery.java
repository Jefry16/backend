package com.vointika.shared.port;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Everything the storefront's globals carry about the shop, read across the
 * context boundary. Implemented in {@code touroperator}.
 *
 * <p><b>Two methods because the request needs the row in two stages.</b> Which
 * locale to render is a decision the storefront makes — bare path serves the
 * primary, a prefix serves a supported secondary, anything else is a 404 — and
 * that decision needs the operator's locales <em>before</em> there is a locale
 * to overlay translations with. So {@link #findLocales} answers the tenant
 * question and feeds the locale rule; {@link #findShop} reads the content once a
 * locale is chosen. Collapsing them into one call means either the adapter
 * applies the locale rule (a storefront policy, in the wrong context) or the
 * caller receives every translation of every field to overlay itself.
 *
 * <p><b>Media are ids, never URLs</b> (PATTERNS §5). The caller resolves them —
 * all of them, in one {@link MediaAssetBatchQuery} call — at read time.
 *
 * <p><b>The plaintext storefront password is not here and must never be.</b>
 * {@code password_message} is public by design (it is shown to a visitor at the
 * gate); the password beside it is a value the operator hands out, which is not
 * the same as one every anonymous request receives.
 */
public interface StorefrontShopQuery {

    /**
     * Stage one: does a storefront live at this handle, and what may it render?
     * Empty when no operator owns the handle — which is the 404, and the only
     * thing the placeholder routes need.
     */
    Optional<ShopLocalesView> findLocales(String handle);

    /**
     * Stage two, once the locale rule has chosen one. Per-locale translations
     * overlay the canonical row <b>nullable-wins-canonical</b>: a translation
     * column that is null falls back rather than blanking the field.
     *
     * @param locale a locale the operator supports; an unsupported one simply
     *               overlays nothing, because the caller has already 404'd
     */
    Optional<ShopView> findShop(UUID tourOperatorId, String locale);

    /** The locale decision's inputs, and the tenant's identity. */
    record ShopLocalesView(UUID tourOperatorId, String primaryLocale, Set<String> supportedLocales) {}

    /**
     * @param policies titles and types only. A footer wants four links and only
     *                 the policy route wants a body, so carrying four HTML
     *                 documents on every page to build links is the wrong trade.
     */
    record ShopView(UUID id,
                    String name,
                    String handle,
                    String address,
                    String phone,
                    String email,
                    String seoTitle,
                    String seoDescription,
                    UUID ogImageMediaId,
                    String passwordMessage,
                    String currencyCode,
                    String currencySymbol,
                    String timezoneName,
                    String timezoneCity,
                    BrandView brand,
                    List<PolicyView> policies) {}

    /**
     * @param primaryColors   ordered by {@code position} — {@code colors.primary[0]}
     *                        is an address a theme indexes into, so the order is
     *                        the query's promise, not the caller's business
     * @param secondaryColors same, split by role <b>here</b> rather than by the
     *                        caller: a role is a {@code touroperator} enum, and a
     *                        role-tagged flat list would make the storefront
     *                        compare against literals it is fenced from
     */
    record BrandView(String slogan,
                     String shortDescription,
                     UUID logoMediaId,
                     UUID squareLogoMediaId,
                     UUID faviconMediaId,
                     UUID coverImageMediaId,
                     List<ColorView> primaryColors,
                     List<ColorView> secondaryColors,
                     List<SocialLinkView> socialLinks) {}

    record ColorView(String background, String foreground) {}

    record SocialLinkView(String platform, String url) {}

    record PolicyView(UUID id, String type, String title) {}
}
