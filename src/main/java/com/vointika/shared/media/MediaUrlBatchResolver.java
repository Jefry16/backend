package com.vointika.shared.media;

import com.vointika.shared.port.MediaKeyBatchQuery;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Resolves a media-id reference to its absolute URL, composing the two seams:
 * {@link MediaKeyBatchQuery} (id → storage key, tenant-scoped) then
 * {@link MediaUrlResolver} (key → URL against the current asset base). The one
 * place consumers (operator logo, and later experience galleries) turn a stored
 * media id back into something the client can load.
 *
 * <p>Tolerant of dangling references: a null id, or an id no longer owned by the
 * operator (deleted, or never valid), resolves to {@code null} — a missing logo
 * stays missing rather than erroring.
 */
@Component
public class MediaUrlBatchResolver {

    private final MediaKeyBatchQuery mediaKeyBatchQuery;
    private final MediaUrlResolver mediaUrlResolver;

    public MediaUrlBatchResolver(MediaKeyBatchQuery mediaKeyBatchQuery, MediaUrlResolver mediaUrlResolver) {
        this.mediaKeyBatchQuery = mediaKeyBatchQuery;
        this.mediaUrlResolver = mediaUrlResolver;
    }

    /** Absolute URL for a single media id owned by the operator, or {@code null}. */
    public String resolveOne(UUID tourOperatorId, UUID mediaId) {
        if (mediaId == null) {
            return null;
        }
        String key = mediaKeyBatchQuery.findKeysByIds(tourOperatorId, Set.of(mediaId)).get(mediaId);
        return key == null ? null : mediaUrlResolver.toUrl(key);
    }

    /**
     * id → absolute URL for many media ids of ONE operator, in a single lookup —
     * the N+1-free path for galleries (an experience's media, a page of
     * experiences' media). Ids not owned by the operator are absent.
     */
    public Map<UUID, String> resolve(UUID tourOperatorId, Collection<UUID> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> keys = mediaKeyBatchQuery.findKeysByIds(tourOperatorId, Set.copyOf(mediaIds));
        Map<UUID, String> urls = new HashMap<>(keys.size());
        keys.forEach((id, key) -> urls.put(id, mediaUrlResolver.toUrl(key)));
        return urls;
    }
}
