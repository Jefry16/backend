package com.vointika.touroperator.application.dto.output;

import com.vointika.touroperator.domain.entity.Brand;
import com.vointika.touroperator.domain.enums.BrandColorRole;

import java.util.List;
import java.util.UUID;

/**
 * The brand, flattened to primitives for the wire.
 *
 * <p>Media are returned as <b>ids</b>, not URLs: this is the editor's view, and
 * the form needs the id it will send back. The storefront resolves the same ids
 * to URLs at read time (PATTERNS §5) — a different concern with a different
 * shape.
 */
public record BrandView(
        String slogan,
        String shortDescription,
        UUID logoMediaId,
        UUID squareLogoMediaId,
        UUID faviconMediaId,
        UUID coverImageMediaId,
        Colors colors,
        List<SocialLink> socialLinks) {

    public record Colors(List<Color> primary, List<Color> secondary) {}

    public record Color(String background, String foreground) {}

    public record SocialLink(String platform, String url) {}

    public static BrandView from(Brand brand) {
        return new BrandView(
                brand.slogan() == null ? null : brand.slogan().value(),
                brand.shortDescription() == null ? null : brand.shortDescription().value(),
                brand.logoMediaId(),
                brand.squareLogoMediaId(),
                brand.faviconMediaId(),
                brand.coverImageMediaId(),
                new Colors(colorsOf(brand, BrandColorRole.PRIMARY),
                        colorsOf(brand, BrandColorRole.SECONDARY)),
                brand.socialLinks().stream()
                        .map(l -> new SocialLink(l.platform().name(), l.url().value()))
                        .toList());
    }

    private static List<Color> colorsOf(Brand brand, BrandColorRole role) {
        return brand.colorsOf(role).stream()
                .map(c -> new Color(c.background().value(), c.foreground().value()))
                .toList();
    }
}
