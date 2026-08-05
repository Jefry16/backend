package com.vointika.storefront.application.dto.output;

import com.vointika.shared.port.MediaAssetBatchQuery.MediaAsset;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontBrandColorView;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontBrandView;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontGateView;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontShopView;
import com.vointika.storefront.application.dto.output.BrandData.ColorData;
import com.vointika.storefront.application.dto.output.BrandData.ColorsData;
import com.vointika.storefront.application.dto.output.BrandData.SocialLinkData;
import com.vointika.storefront.application.dto.output.LocalizationData.LanguageData;
import com.vointika.storefront.application.dto.output.ShopData.CurrencyData;
import com.vointika.storefront.application.dto.output.ShopData.TimezoneData;

import java.util.Comparator;
import java.util.List;

/**
 * The envelope every storefront page receives, whatever else it carries: the
 * shop, this page's own SEO values, and the language this request resolved to.
 *
 * <p><b>This is a published contract.</b> The moment operators author their own
 * themes, {@code shop.name} is API — renaming a field here breaks every theme
 * written against it — which is why the shape is decided once, now, rather than
 * migrated later. Shopify's global objects are the reference deliberately: a
 * theme author who has seen one storefront should recognise these.
 *
 * <p>The home page <em>is</em> this and nothing more, which is why
 * {@code GetHomePageUseCase} returns it directly rather than through a record of
 * its own. Shopify's {@code index} template is the same shape for the same
 * reason — a home page has no object the envelope does not already hold.
 *
 * <p>{@code routes} is the fourth object a template sees and is absent here on
 * purpose: a route is a URL, and this layer deals in storage keys and locale
 * codes. Presentation builds both.
 */
public record StorefrontPageData(ShopData shop, PageData page, LocalizationData localization) {

    /**
     * @param locale the locale the caller resolved and asked {@code shop} for —
     *               the one the page is actually being rendered in.
     */
    public static StorefrontPageData from(StorefrontGateView gate, StorefrontShopView shop, String locale) {
        return new StorefrontPageData(
                new ShopData(
                        gate.tourOperatorId(),
                        shop.name(),
                        shop.address(),
                        shop.phone(),
                        shop.email(),
                        shop.seoDescription(),
                        shop.passwordMessage(),
                        brand(shop.brand()),
                        new CurrencyData(shop.currencyCode(), shop.currencySymbol()),
                        new TimezoneData(shop.timezoneName(), shop.timezoneCity())),
                new PageData(
                        // The shop's SEO title is the page's title until a page type
                        // carries one of its own; its name is the last fallback.
                        shop.seoTitle() == null ? shop.name() : shop.seoTitle(),
                        shop.seoDescription(),
                        shop.ogImageKey()),
                new LocalizationData(locale, languages(gate, locale)));
    }

    /**
     * A straight re-shaping: the port already answers the palette split by role
     * and ordered within each, because a role is {@code touroperator}'s enum and
     * cannot cross the seam. Nothing is decided here.
     */
    private static BrandData brand(StorefrontBrandView brand) {
        return new BrandData(
                brand.slogan(),
                brand.shortDescription(),
                new ColorsData(colors(brand.colors().primary()), colors(brand.colors().secondary())),
                image(brand.logo()),
                image(brand.squareLogo()),
                image(brand.favicon()),
                image(brand.coverImage()),
                brand.socialLinks().stream()
                        .map(link -> new SocialLinkData(link.platform(), link.url()))
                        .toList());
    }

    private static List<ColorData> colors(List<StorefrontBrandColorView> colors) {
        return colors.stream()
                .map(color -> new ColorData(color.background(), color.foreground()))
                .toList();
    }

    /** An unset — or since-deleted — media reference is no image at all, not an empty one. */
    private static ImageData image(MediaAsset asset) {
        return asset == null
                ? null
                : new ImageData(asset.storageKey(), asset.alt(), asset.width(), asset.height());
    }

    /**
     * The primary leads and the rest follow alphabetically — a switcher that
     * reorders between requests reads as a bug, and {@code supportedLocales} is a
     * {@code Set} with no order of its own.
     */
    private static List<LanguageData> languages(StorefrontGateView gate, String locale) {
        Comparator<String> primaryFirst = Comparator
                .comparing((String code) -> !code.equals(gate.primaryLocale()))
                .thenComparing(Comparator.naturalOrder());
        return gate.supportedLocales().stream()
                .sorted(primaryFirst)
                .map(code -> new LanguageData(
                        code,
                        code.equals(locale),
                        code.equals(gate.primaryLocale()) ? null : code))
                .toList();
    }
}
