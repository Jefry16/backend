package com.vointika.storefront.presentation.response;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.port.MediaAssetBatchQuery.MediaAsset;
import com.vointika.shared.port.StorefrontShopQuery.BrandView;
import com.vointika.shared.port.StorefrontShopQuery.ColorView;
import com.vointika.shared.port.StorefrontShopQuery.PolicyView;
import com.vointika.shared.port.StorefrontShopQuery.ShopView;
import com.vointika.storefront.application.dto.output.StorefrontGlobals;
import com.vointika.storefront.application.policy.StorefrontRoutes;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What every storefront address answers with, and — until a route has an object
 * of its own — the entire home page.
 *
 * <p><b>Names follow Shopify's, and renaming one is a breaking change</b> the day
 * an operator authors a theme. That is why the shape is decided here, against
 * JSON, while it costs a handful of records instead of a pile of templates.
 * Three deliberate departures, all recorded rather than accidental:
 *
 * <ul>
 *   <li><b>camelCase, not Liquid's snake_case.</b> A theme author arriving from
 *       Shopify types {@code short_description}; every other surface this project
 *       serves is camelCase, and one convention across the codebase beat one
 *       convention across an ecosystem we do not belong to.
 *   <li><b>{@code pageTitle}/{@code pageDescription} are top-level, and there is
 *       no {@code page} object.</b> In Shopify {@code page} is a CMS page, and it
 *       means the same thing here — so the current page's metadata uses their
 *       {@code page_title}/{@code page_description} globals and leaves the name
 *       free for {@code /pages/&#123;handle&#125;}.
 * </ul>
 *
 * <p><b>{@code ogImageUrl} has no Shopify counterpart</b>, and that is the
 * third departure: Shopify writes its own Open Graph tags through
 * {@code content_for_header}, which we have no equivalent of, so a theme needs
 * the value itself.
 */
public record StorefrontGlobalsResponse(Shop shop,
                                        String pageTitle,
                                        String pageDescription,
                                        String ogImageUrl,
                                        Routes routes,
                                        Localization localization) {

    /**
     * @param description the meta description, which is what Shopify's
     *                    {@code shop.description} is too — the same value
     *                    {@code pageDescription} carries on a page that has no
     *                    override of its own
     * @param passwordMessage public by design: it is shown to a visitor at the
     *                        gate. The password beside it in the database is not
     *                        here and must never be.
     */
    public record Shop(UUID id,
                       String name,
                       String handle,
                       String url,
                       String description,
                       String address,
                       String phone,
                       String email,
                       String passwordMessage,
                       Currency currency,
                       Timezone timezone,
                       Brand brand,
                       List<Policy> policies,
                       Policy cancellationPolicy,
                       Policy privacyPolicy,
                       Policy termsOfService,
                       Policy legalNotice) {}

    public record Currency(String code, String symbol) {}

    public record Timezone(String name, String city) {}

    public record Brand(String slogan,
                        String shortDescription,
                        Colors colors,
                        Image logo,
                        Image squareLogo,
                        Image favicon,
                        Image coverImage,
                        List<SocialLink> socialLinks) {}

    public record Colors(List<Color> primary, List<Color> secondary) {}

    public record Color(String background, String foreground) {}

    public record SocialLink(String platform, String url) {}

    /**
     * Everything a theme renders as an {@code <img>}.
     *
     * <p>{@code aspectRatio} is <b>derived</b> and never stored — a third column
     * can disagree with the two it comes from. An absent media reference is a
     * <b>null Image</b> rather than an Image with a null url, so a template
     * guards on the object.
     */
    public record Image(String url, String alt, Integer width, Integer height, Double aspectRatio) {}

    /**
     * @param type the enum name; {@code url} carries the slug, because
     *             {@code LEGAL_NOTICE} addresses {@code /policies/legal-notice}
     */
    public record Policy(UUID id, String type, String title, String url) {}

    public record Routes(String root, String experiences) {}

    /**
     * @param language  the one being served
     * @param languages every locale the shop publishes, primary first
     */
    public record Localization(Language language, List<Language> languages) {}

    /**
     * @param name        the language written in the shop's primary locale —
     *                    "francés" for a Spanish shop. Shopify's {@code name}.
     * @param endonymName the language written in itself — "français"
     * @param primary     whether this is the shop's primary locale, <b>not</b>
     *                    whether it is the one being served: that is
     *                    {@code localization.language}
     */
    public record Language(String code, String name, String endonymName, boolean primary, String url) {}

    /**
     * Every media id the response will need, so the caller can resolve them in
     * <b>one</b> batch rather than one lookup per image. Kept beside
     * {@link #from} so the two cannot drift apart.
     */
    public static Set<UUID> mediaIds(StorefrontGlobals globals) {
        Set<UUID> ids = new LinkedHashSet<>();
        add(ids, globals.ogImageMediaId());
        BrandView brand = globals.shop().brand();
        add(ids, brand.logoMediaId());
        add(ids, brand.squareLogoMediaId());
        add(ids, brand.faviconMediaId());
        add(ids, brand.coverImageMediaId());
        return ids;
    }

    /**
     * @param origin scheme and host of the request, which is what {@code shop.url}
     *               is. Taken from the request rather than from configuration so
     *               it stays correct behind a proxy, the same reason the tenant is
     *               read from {@code getServerName()}.
     */
    public static StorefrontGlobalsResponse from(StorefrontGlobals globals,
                                                 String origin,
                                                 Map<UUID, MediaAsset> assets,
                                                 MediaUrlResolver urls) {
        ShopView shop = globals.shop();
        String prefix = localePrefix(globals);
        List<Policy> policies = shop.policies().stream()
                .map(p -> policy(p, prefix))
                .toList();
        Image ogImage = image(globals.ogImageMediaId(), assets, urls);

        return new StorefrontGlobalsResponse(
                new Shop(
                        shop.id(),
                        shop.name(),
                        shop.handle(),
                        origin,
                        shop.seoDescription(),
                        shop.address(),
                        shop.phone(),
                        shop.email(),
                        shop.passwordMessage(),
                        new Currency(shop.currencyCode(), shop.currencySymbol()),
                        new Timezone(shop.timezoneName(), shop.timezoneCity()),
                        brand(shop.brand(), assets, urls),
                        policies,
                        named(policies, "CANCELLATION"),
                        named(policies, "PRIVACY"),
                        named(policies, "TERMS"),
                        named(policies, "LEGAL_NOTICE")),
                globals.pageTitle(),
                globals.pageDescription(),
                ogImage == null ? null : ogImage.url(),
                new Routes(prefix.isEmpty() ? StorefrontRoutes.HOME : prefix,
                        prefix + StorefrontRoutes.EXPERIENCES),
                localization(globals));
    }

    private static Brand brand(BrandView brand, Map<UUID, MediaAsset> assets, MediaUrlResolver urls) {
        return new Brand(
                brand.slogan(),
                brand.shortDescription(),
                new Colors(colors(brand.primaryColors()), colors(brand.secondaryColors())),
                image(brand.logoMediaId(), assets, urls),
                image(brand.squareLogoMediaId(), assets, urls),
                image(brand.faviconMediaId(), assets, urls),
                image(brand.coverImageMediaId(), assets, urls),
                brand.socialLinks().stream()
                        .map(l -> new SocialLink(l.platform(), l.url()))
                        .toList());
    }

    private static List<Color> colors(List<ColorView> views) {
        return views.stream().map(c -> new Color(c.background(), c.foreground())).toList();
    }

    private static Image image(UUID mediaId, Map<UUID, MediaAsset> assets, MediaUrlResolver urls) {
        if (mediaId == null) {
            return null;
        }
        MediaAsset asset = assets.get(mediaId);
        if (asset == null) {
            return null;
        }
        return new Image(urls.toUrl(asset.storageKey()), asset.alt(), asset.width(), asset.height(),
                aspectRatio(asset.width(), asset.height()));
    }

    private static Double aspectRatio(Integer width, Integer height) {
        if (width == null || height == null || height == 0) {
            return null;
        }
        return (double) width / height;
    }

    private static Policy policy(PolicyView view, String prefix) {
        String slug = view.type().toLowerCase(Locale.ROOT).replace('_', '-');
        return new Policy(view.id(), view.type(), view.title(),
                prefix + StorefrontRoutes.POLICIES + "/" + slug);
    }

    private static Policy named(List<Policy> policies, String type) {
        return policies.stream().filter(p -> p.type().equals(type)).findFirst().orElse(null);
    }

    /**
     * The switcher needs "this page in that language". On the home page that is
     * each locale's root, so it is built here; the first route whose path varies
     * per page is the one that has to pass its own.
     */
    private static Localization localization(StorefrontGlobals globals) {
        StorefrontGlobals.LocalizationData data = globals.localization();
        List<Language> languages = new ArrayList<>();
        for (String code : data.supported()) {
            languages.add(language(code, data.primary(), data.primary().equals(code) ? "/" : "/" + code));
        }
        Language current = languages.stream()
                .filter(l -> l.code().equals(data.current()))
                .findFirst()
                .orElse(null);
        return new Localization(current, List.copyOf(languages));
    }

    /**
     * Both names come from the JDK's CLDR data rather than a curated column.
     * Shopify's {@code name} is the language rendered <em>in the shop's primary
     * locale</em>, which is one value per <em>pair</em> of languages — six
     * languages is thirty-six of them, so it is not a table anyone maintains.
     * {@code getDisplayName} rather than {@code getDisplayLanguage} because it
     * keeps the region: {@code pt-BR} and {@code pt-PT} are both "português"
     * without it, which makes a two-Portuguese switcher unusable. An unknown code
     * comes back as itself, so nothing here can produce a null.
     */
    private static Language language(String code, String primaryLocale, String url) {
        Locale locale = Locale.forLanguageTag(code);
        return new Language(
                code,
                locale.getDisplayName(Locale.forLanguageTag(primaryLocale)),
                locale.getDisplayName(locale),
                code.equals(primaryLocale),
                url);
    }

    private static String localePrefix(StorefrontGlobals globals) {
        StorefrontGlobals.LocalizationData data = globals.localization();
        return data.current().equals(data.primary()) ? "" : "/" + data.current();
    }

    private static void add(Set<UUID> ids, UUID id) {
        if (id != null) {
            ids.add(id);
        }
    }
}
