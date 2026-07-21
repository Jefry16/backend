package com.vointika.media.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A validated upload content type. Normalizes the raw header (strips MIME
 * parameters, lowercases, trims) and allowlist-checks it — the media library
 * accepts raster images + PDF only. SVG is deliberately excluded (script
 * injection); video is deferred until a consumer needs it.
 *
 * <p>The extension is derived from the type so storage keys carry a sensible
 * suffix regardless of the original filename.
 */
public final class ContentType {

    private static final Map<String, String> EXTENSION_BY_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "application/pdf", "pdf");

    static final Set<String> ALLOWED = EXTENSION_BY_TYPE.keySet();

    private final String value;

    public ContentType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidFieldException("Content type is required");
        }
        int semicolon = raw.indexOf(';');
        String bare = (semicolon >= 0 ? raw.substring(0, semicolon) : raw).trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED.contains(bare)) {
            throw new InvalidFieldException(
                    "Unsupported content type: allowed image/jpeg, image/png, image/webp, application/pdf");
        }
        this.value = bare;
    }

    public String value() {
        return value;
    }

    /** File extension for this type (e.g. {@code jpg}) — for the storage key suffix. */
    public String extension() {
        return EXTENSION_BY_TYPE.get(value);
    }
}
