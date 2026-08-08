package com.vointika.media.infrastructure.port;

import com.vointika.shared.port.DiagnosticLogPort;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * The adapter that exists because {@code javax.imageio} is outside the
 * application layer's allowlist.
 *
 * <p>Its contract is narrow and mostly about <b>not</b> doing things: never
 * throw, never decode the pixels, and answer empty for anything that is not a
 * measurable image. An upload must survive all of it.
 */
class ImageIoImageDimensionsTest {

    private final ImageIoImageDimensions dimensions =
            new ImageIoImageDimensions(mock(DiagnosticLogPort.class));

    private static byte[] png(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static InputStream stream(byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }

    @Test
    void measuresARealImage() throws IOException {
        var result = dimensions.measure(stream(png(400, 200)), "image/png");

        assertThat(result).isPresent();
        assertThat(result.get().width()).isEqualTo(400);
        assertThat(result.get().height()).isEqualTo(200);
    }

    @Test
    void measuresJpegAsWellAsPng() throws IOException {
        BufferedImage image = new BufferedImage(120, 60, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);

        var result = dimensions.measure(stream(out.toByteArray()), "image/jpeg");

        assertThat(result).isPresent();
        assertThat(result.get().width()).isEqualTo(120);
    }

    @Test
    void aPdfIsEmptyRatherThanAnError() {
        // The media allowlist includes PDF, which has no pixel dimensions. This is
        // the ordinary case for a non-image upload, not a failure.
        byte[] pdf = "%PDF-1.7\nnot really a pdf".getBytes();

        assertThat(dimensions.measure(stream(pdf), "application/pdf")).isEmpty();
    }

    @Test
    void aContentTypeThatIsNotAnImageIsNotEvenRead() {
        assertThat(dimensions.measure(stream(new byte[]{1, 2, 3}), "application/pdf")).isEmpty();
        assertThat(dimensions.measure(stream(new byte[]{1, 2, 3}), null)).isEmpty();
    }

    @Test
    void aTruncatedImageIsEmptyAndDoesNotThrow() throws IOException {
        // The failure that would otherwise take an upload down with it: a file
        // that claims to be a PNG and whose header stops halfway.
        byte[] full = png(400, 200);
        byte[] truncated = new byte[12];
        System.arraycopy(full, 0, truncated, 0, 12);

        assertThat(dimensions.measure(stream(truncated), "image/png")).isEmpty();
    }

    @Test
    void garbageClaimingToBeAnImageIsEmptyAndDoesNotThrow() {
        byte[] garbage = "this is definitely not a png".getBytes();

        assertThat(dimensions.measure(stream(garbage), "image/png")).isEmpty();
    }

    @Test
    void anEmptyStreamIsEmptyAndDoesNotThrow() {
        assertThat(dimensions.measure(stream(new byte[0]), "image/png")).isEmpty();
    }
}
