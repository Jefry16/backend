package com.vointika.storefront.presentation.view;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.storefront.application.dto.output.BrandData;

import java.util.List;

/**
 * {@code {{shop.brand}}} — Shopify's brand object, modelled faithfully, and the
 * only place a logo lives: {@code shop.logoUrl} is gone because their shop
 * object never had one.
 *
 * <p><b>Never null</b>, even for an operator with no brand row — a theme's
 * {@code {{#shop.brand.logo}}} then means "no logo" rather than "no brand", and
 * the two must not be different shapes.
 *
 * <p>Public, and so is every record nested here, for the reason {@link Shop}
 * documents.
 *
 * <p>One deliberate deviation from Shopify: their social links ride
 * {@code brand.metafields}. Ours are first-class, because every metaobject
 * definition here is operator-authored and none is guaranteed to exist — a theme
 * reading a social link off one would be reading a coincidence.
 */
public record Brand(String slogan, String shortDescription, Colors colors,
                    Image logo, Image squareLogo, Image favicon, Image coverImage,
                    List<SocialLink> socialLinks) {

    /**
     * @param primary each role is an <em>ordered list</em>, not a value — this is
     *                {@code colors.primary[0].background}, and the order is the
     *                operator's, resolved by position in the query
     */
    public record Colors(List<Color> primary, List<Color> secondary) {}

    public record Color(String background, String foreground) {}

    /** @param platform an uppercase name a theme switches an icon on. */
    public record SocialLink(String platform, String url) {}

    public static Brand from(BrandData brand, MediaUrlResolver mediaUrlResolver) {
        return new Brand(
                brand.slogan(),
                brand.shortDescription(),
                new Colors(colors(brand.colors().primary()), colors(brand.colors().secondary())),
                Image.from(brand.logo(), mediaUrlResolver),
                Image.from(brand.squareLogo(), mediaUrlResolver),
                Image.from(brand.favicon(), mediaUrlResolver),
                Image.from(brand.coverImage(), mediaUrlResolver),
                brand.socialLinks().stream()
                        .map(link -> new SocialLink(link.platform(), link.url()))
                        .toList());
    }

    private static List<Color> colors(List<BrandData.ColorData> colors) {
        return colors.stream().map(color -> new Color(color.background(), color.foreground())).toList();
    }
}
