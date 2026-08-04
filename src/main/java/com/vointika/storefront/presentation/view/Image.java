package com.vointika.storefront.presentation.view;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.storefront.application.dto.output.ImageData;

/**
 * {@code {{…}}} for anything a theme renders as an {@code <img>} — Shopify's
 * image object, and shared rather than repeated because the brand already has
 * four of them and the experience card is a fifth the moment it needs
 * dimensions.
 *
 * <p>Public for the reason {@link Shop} documents.
 *
 * @param alt         <b>null on every row today.</b> The column exists and
 *                    nothing populates it yet — only the uploader knows an alt
 *                    text, so it needs an admin field. A theme should still read
 *                    it rather than invent one.
 * @param width       null for the same reason, and for one more: dimensions have
 *                    to be measured at upload, which needs a port.
 * @param aspectRatio <b>derived, never stored</b> — width over height when both
 *                    are known, null otherwise. A third column would be a value
 *                    that can disagree with the two it comes from.
 */
public record Image(String url, String alt, Integer width, Integer height, Double aspectRatio) {

    public static Image from(ImageData image, MediaUrlResolver mediaUrlResolver) {
        if (image == null) {
            return null;
        }
        return new Image(
                mediaUrlResolver.toUrl(image.storageKey()),
                image.alt(),
                image.width(),
                image.height(),
                aspectRatio(image.width(), image.height()));
    }

    private static Double aspectRatio(Integer width, Integer height) {
        if (width == null || height == null) {
            return null;
        }
        return (double) width / height;
    }
}
