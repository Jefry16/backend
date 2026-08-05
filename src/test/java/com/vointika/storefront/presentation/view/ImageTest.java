package com.vointika.storefront.presentation.view;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.storefront.application.dto.output.ImageData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImageTest {

    private final MediaUrlResolver mediaUrlResolver = new MediaUrlResolver("https://cdn.example.com");

    @Test
    void resolvesTheKeyAndCarriesTheDescriptiveFieldsThrough() {
        Image image = Image.from(new ImageData("brand/logo.png", "The Acme burgee", 400, 200), mediaUrlResolver);

        assertThat(image.url()).isEqualTo("https://cdn.example.com/brand/logo.png");
        assertThat(image.alt()).isEqualTo("The Acme burgee");
        assertThat(image.width()).isEqualTo(400);
        assertThat(image.height()).isEqualTo(200);
    }

    /** Derived, never stored — a third column could disagree with the two it comes from. */
    @Test
    void theAspectRatioIsDerivedWhenBothDimensionsAreKnown() {
        assertThat(Image.from(new ImageData("k", null, 400, 200), mediaUrlResolver).aspectRatio())
                .isEqualTo(2.0);
        assertThat(Image.from(new ImageData("k", null, 1920, 1080), mediaUrlResolver).aspectRatio())
                .isEqualTo(16.0 / 9.0);
    }

    /**
     * <b>The state every row is in today</b> — the media columns exist and nothing
     * populates them. Null rather than a guessed 1.0, because a theme guarding
     * {@code {{#aspectRatio}}} must be able to tell "square" from "unknown".
     */
    @Test
    void theAspectRatioIsNullWhenEitherDimensionIsMissing() {
        assertThat(Image.from(new ImageData("k", null, 400, null), mediaUrlResolver).aspectRatio()).isNull();
        assertThat(Image.from(new ImageData("k", null, null, 200), mediaUrlResolver).aspectRatio()).isNull();
        assertThat(Image.from(new ImageData("k", null, null, null), mediaUrlResolver).aspectRatio()).isNull();
    }

    /** No media reference is no image, not an image with a null URL — the template guards on the object. */
    @Test
    void anAbsentImageIsNull() {
        assertThat(Image.from(null, mediaUrlResolver)).isNull();
    }
}
