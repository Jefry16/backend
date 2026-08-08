package com.vointika.media.application.port;

import java.io.InputStream;
import java.util.Optional;

/**
 * Measures an image's pixel dimensions.
 *
 * <p><b>A port rather than a call, because {@code javax.imageio} is not
 * {@code java.*}.</b> The application layer's ArchUnit allowlist is
 * {@code com.vointika..} + {@code java..}, and {@code javax} matches neither —
 * so {@code ImageIO.read(...)} in a use case fails the build exactly as a
 * third-party jar would. The same trap {@code javax.crypto} sprang on the
 * storefront's unlock cookie (PATTERNS §8d).
 *
 * <p><b>Empty is a normal answer, not a failure.</b> The media allowlist
 * includes PDF, which has no pixel dimensions; a format the decoder does not
 * know, or a file whose header is damaged, answers the same way. The columns
 * stay null and the storefront's {@code Image.aspectRatio} keeps deriving to
 * null, which every template already guards for. The adapter logs and swallows
 * — an image that cannot be measured is still a usable upload, so this must
 * never fail the upload.
 */
public interface ImageDimensionsPort {

    /** Pixel size, or empty when this stream is not a measurable image. */
    Optional<Dimensions> measure(InputStream body, String contentType);

    record Dimensions(int width, int height) {}
}
