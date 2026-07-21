package com.vointika.shared.media;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MediaUrlResolverTest {

    private static final String BASE = "https://media.staging.vointika.com";
    private final MediaUrlResolver resolver = new MediaUrlResolver(BASE);

    @Test
    void buildsUrlFromRelativeKey() {
        assertEquals(
                BASE + "/tour-operators/op/abc-photo.jpg",
                resolver.toUrl("tour-operators/op/abc-photo.jpg"));
    }

    @Test
    void rehostsAbsoluteUrlFromAnyHostToCurrentBase() {
        assertEquals(
                BASE + "/tour-operators/op/abc-photo.jpg",
                resolver.toUrl("https://pub-old.r2.dev/tour-operators/op/abc-photo.jpg"));
    }

    @Test
    void isIdempotentForCurrentBase() {
        String url = BASE + "/tour-operators/op/abc-photo.jpg";
        assertEquals(url, resolver.toUrl(url));
    }

    @Test
    void stripsLeadingSlashOnRelativeKey() {
        assertEquals(
                BASE + "/tour-operators/op/x.jpg",
                resolver.toUrl("/tour-operators/op/x.jpg"));
    }

    @Test
    void normalizesTrailingSlashOnBase() {
        MediaUrlResolver withSlash = new MediaUrlResolver(BASE + "/");
        assertEquals(BASE + "/x.jpg", withSlash.toUrl("x.jpg"));
    }

    @Test
    void nullAndBlankPassThrough() {
        assertNull(resolver.toUrl(null));
        assertEquals("", resolver.toUrl(""));
    }
}
