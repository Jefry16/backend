package com.vointika.touroperator.presentation.response;

import com.vointika.touroperator.application.dto.output.BrandView;

import java.util.List;
import java.util.UUID;

/**
 * The operator's brand. A settings sub-resource — one per operator, addressed by
 * the operator — so no {@code id}/{@code context} envelope (PATTERNS §4a), the
 * shape {@code OperatorSeoResponse} and the locales response use.
 *
 * <p>Media come back as <b>ids</b>: this is what the editor sends back. The
 * storefront resolves the same ids to URLs at read time.
 */
public record BrandResponse(
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

    public static BrandResponse from(BrandView view) {
        return new BrandResponse(
                view.slogan(), view.shortDescription(),
                view.logoMediaId(), view.squareLogoMediaId(),
                view.faviconMediaId(), view.coverImageMediaId(),
                new Colors(map(view.colors().primary()), map(view.colors().secondary())),
                view.socialLinks().stream()
                        .map(l -> new SocialLink(l.platform(), l.url()))
                        .toList());
    }

    private static List<Color> map(List<BrandView.Color> colors) {
        return colors.stream().map(c -> new Color(c.background(), c.foreground())).toList();
    }
}
