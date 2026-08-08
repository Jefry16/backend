package com.vointika.touroperator.application.dto.input;

import java.util.List;
import java.util.UUID;

/**
 * The whole brand. <b>A full replace</b>: an absent field clears the value and
 * absent collections empty them, the way every other {@code PUT} in this API
 * behaves. A brand editor sends the whole object on every save.
 */
public record UpdateBrandInput(
        String slogan,
        String shortDescription,
        UUID logoMediaId,
        UUID squareLogoMediaId,
        UUID faviconMediaId,
        UUID coverImageMediaId,
        Colors colors,
        List<SocialLink> socialLinks) {

    /** The palette, split by role the way a theme reads it. Order is significant. */
    public record Colors(List<Color> primary, List<Color> secondary) {}

    public record Color(String background, String foreground) {}

    public record SocialLink(String platform, String url) {}
}
