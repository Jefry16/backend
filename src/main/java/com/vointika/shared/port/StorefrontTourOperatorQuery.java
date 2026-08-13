package com.vointika.shared.port;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Everything the storefront's globals carry about the operator, read across the
 * context boundary. Implemented in {@code touroperator}.
 *
 * <p><b>Two methods because the request needs the row in two stages.</b> Which
 * locale to render is a decision the storefront makes — bare path serves the
 * primary, a prefix serves a supported secondary, anything else is a 404 — and
 * that decision needs the operator's locales <em>before</em> there is a locale
 * to overlay translations with. So {@link #findLocales} answers the tenant
 * question and feeds the locale rule; {@link #findOperator} reads the content once a
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
public interface StorefrontTourOperatorQuery {

    /**
     * Stage one: does a storefront live at this handle, and what may it render?
     * Empty when no operator owns the handle — which is the 404, and the only
     * thing the placeholder routes need.
     */
    Optional<LocalesView> findLocales(String handle);

    /**
     * The gate's configuration, asked <b>before any locale exists</b> — the gate
     * runs before locale resolution, and that ordering is the whole point of it.
     *
     * <p>It is a second read of the same row rather than fields on
     * {@link LocalesView} on purpose: this one carries the plaintext
     * password, and keeping it out of the read that builds the public payload
     * means no future edit to the globals can leak it by accident.
     */
    Optional<GateView> findGate(String handle);

    /**
     * Stage two, once the locale rule has chosen one. Per-locale translations
     * overlay the canonical row <b>nullable-wins-canonical</b>: a translation
     * column that is null falls back rather than blanking the field.
     *
     * @param locale a locale the operator supports; an unsupported one simply
     *               overlays nothing, because the caller has already 404'd
     */
    Optional<TourOperatorView> findOperator(UUID tourOperatorId, String locale);

    /** The locale decision's inputs, and the tenant's identity. */
    record LocalesView(UUID tourOperatorId, String primaryLocale, Set<String> supportedLocales) {}

    /**
     * @param operatorName        not translated — V8 left {@code name} off the overlay
     *                        deliberately, because a brand name is not content
     * @param passwordMessage already overlaid from the <b>primary-locale</b>
     *                        translation. The gate has no locale of its own: it
     *                        runs before one is chosen, and choosing one first is
     *                        the leak the ordering exists to prevent.
     */
    record GateView(UUID tourOperatorId,
                    String operatorName,
                    boolean passwordEnabled,
                    String storefrontPassword,
                    String passwordMessage) {}

    /**
     * @param policies titles and types only. A footer wants four links and only
     *                 the policy route wants a body, so carrying four HTML
     *                 documents on every page to build links is the wrong trade.
     */
    record TourOperatorView(UUID id,
                    String name,
                    String handle,
                    AddressView address,
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

    /**
     * Flat primitives, with the country already resolved — the port's contract is
     * primitives, and the Shopify-shaped nesting is the response's decision, made
     * once where the payload is assembled.
     *
     * <p>Null for an operator that has no address. Every operator created before
     * the structured-address slice is in that state.
     */
    record AddressView(String address1,
                       String address2,
                       String street,
                       String city,
                       String province,
                       String zip,
                       String countryCode,
                       String countryName) {}

    record ColorView(String background, String foreground) {}

    record SocialLinkView(String platform, String url) {}

    record PolicyView(UUID id, String type, String title) {}
}
