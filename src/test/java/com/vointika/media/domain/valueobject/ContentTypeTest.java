package com.vointika.media.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContentTypeTest {

    @Test
    void acceptsAllowedTypesAndDerivesExtension() {
        assertEquals("jpg", new ContentType("image/jpeg").extension());
        assertEquals("png", new ContentType("image/png").extension());
        assertEquals("webp", new ContentType("image/webp").extension());
        assertEquals("pdf", new ContentType("application/pdf").extension());
    }

    @Test
    void normalizesParametersCaseAndWhitespace() {
        ContentType ct = new ContentType("  IMAGE/PNG; charset=utf-8 ");
        assertEquals("image/png", ct.value());
    }

    @Test
    void rejectsDisallowedTypes() {
        assertThrows(InvalidFieldException.class, () -> new ContentType("text/plain"));
        assertThrows(InvalidFieldException.class, () -> new ContentType("image/svg+xml"));
        assertThrows(InvalidFieldException.class, () -> new ContentType("video/mp4"));
    }

    @Test
    void rejectsBlank() {
        assertThrows(InvalidFieldException.class, () -> new ContentType(null));
        assertThrows(InvalidFieldException.class, () -> new ContentType("  "));
    }
}
