package com.vointika.media.infrastructure.port;

import com.vointika.media.application.port.ImageDimensionsPort;
import com.vointika.shared.port.DiagnosticLogPort;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Optional;

/**
 * Reads pixel dimensions with {@code javax.imageio} — the reason
 * {@link ImageDimensionsPort} exists at all, since {@code javax} is outside the
 * application layer's allowlist.
 *
 * <p><b>Header only, never the pixels.</b> {@code reader.getWidth(0)} decodes
 * enough of the stream to answer and stops; {@code ImageIO.read} would decode
 * the entire image into a {@code BufferedImage}, which for a 25 MB upload is
 * tens of megabytes of heap for two integers.
 *
 * <p><b>Never throws.</b> A PDF, an unknown format, a truncated header — all
 * answer empty, because an image that cannot be measured is still a perfectly
 * good upload and failing here would fail the upload. That is the "adapter
 * swallows and logs" rule for a side effect the caller does not depend on
 * (PATTERNS §8d).
 */
@Component
public class ImageIoImageDimensions implements ImageDimensionsPort {

    private final DiagnosticLogPort diagnosticLog;

    public ImageIoImageDimensions(DiagnosticLogPort diagnosticLog) {
        this.diagnosticLog = diagnosticLog;
    }

    @Override
    public Optional<Dimensions> measure(InputStream body, String contentType) {
        if (contentType == null || !contentType.startsWith("image/")) {
            return Optional.empty();
        }
        try (ImageInputStream stream = ImageIO.createImageInputStream(body)) {
            if (stream == null) {
                return Optional.empty();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                // A content type we allow but ImageIO has no plugin for — SVG
                // would be this, if the allowlist had not already excluded it.
                return Optional.empty();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                return width > 0 && height > 0
                        ? Optional.of(new Dimensions(width, height))
                        : Optional.empty();
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            // Deliberately broad: a malformed header can surface as almost
            // anything from a decoder plugin, and none of it is worth failing an
            // upload over.
            diagnosticLog.warn(getClass(),
                    "Could not measure image dimensions (contentType=" + contentType
                            + "); storing without them", e);
            return Optional.empty();
        }
    }
}
