package com.vointika.shared.media;

import java.util.List;

/**
 * Resolves a stored media reference to an absolute public URL against the
 * <em>current</em> media bucket base URL ({@code app.s3.public-base-url}).
 *
 * <p>Media references are persisted in several contexts (the media library,
 * an experience's thumbnail/gallery, a tour operator's logo). To keep those
 * base-URL-independent — so changing the asset domain never requires a data
 * migration — the absolute URL is built at <strong>read time</strong> here
 * rather than frozen into the row at write time.
 *
 * <p>{@link #toUrl} is tolerant of whatever shape the stored value takes:
 * a relative storage key ({@code tour-operators/…/x.jpg}) or a previously
 * absolute URL on any host ({@code https://old-host/tour-operators/…/x.jpg}).
 * Both resolve to {@code <currentBase>/tour-operators/…/x.jpg}. This makes the
 * transform idempotent and self-healing across base-URL changes.
 *
 * <p>{@code null}/blank passes through unchanged (a missing thumbnail stays
 * missing).
 */
public class MediaUrlResolver {

    private final String baseNoTrailingSlash;

    public MediaUrlResolver(String publicBaseUrl) {
        this.baseNoTrailingSlash = stripTrailingSlash(publicBaseUrl);
    }

    /** Absolute URL for a stored reference, re-hosted to the current base. */
    public String toUrl(String stored) {
        if (stored == null || stored.isBlank()) {
            return stored;
        }
        return baseNoTrailingSlash + "/" + toKey(stored);
    }

    /** Same as {@link #toUrl} over a list; {@code null} list passes through. */
    public List<String> toUrls(List<String> stored) {
        if (stored == null) {
            return null;
        }
        return stored.stream().map(this::toUrl).toList();
    }

    /**
     * The host-independent relative key: strips a leading {@code scheme://host}
     * (and any leading slash) so only the object path remains.
     */
    public String toKey(String stored) {
        if (stored == null || stored.isBlank()) {
            return stored;
        }
        String s = stored;
        int scheme = s.indexOf("://");
        if (scheme >= 0) {
            int pathStart = s.indexOf('/', scheme + 3);
            s = (pathStart >= 0) ? s.substring(pathStart + 1) : "";
        } else if (s.startsWith("/")) {
            s = s.substring(1);
        }
        return s;
    }

    private static String stripTrailingSlash(String url) {
        return (url != null && url.endsWith("/")) ? url.substring(0, url.length() - 1) : url;
    }
}
