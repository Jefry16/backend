package com.vointika.shared.port;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Cross-context resolution of media ids to the stored asset, scoped to an
 * operator. The seam other contexts (experience galleries, the operator's brand)
 * use to turn stored media-id references back into something renderable — then
 * {@code MediaUrlResolver} turns the storage key into an absolute URL at read
 * time.
 *
 * <p><b>Tenant-scoped:</b> an id not owned by {@code tourOperatorId} is simply
 * absent from the result — a consumer treats a miss as an invalid reference
 * (validation → 422) and never trusts a cross-tenant id. Implemented in
 * {@code media.infrastructure.query}.
 *
 * <p>It answered {@code Map<UUID, String>} until brand images needed more than a
 * key. One method returning the whole asset rather than a second method beside
 * the first: a caller that only asks <em>does this id belong to this
 * operator</em> still reads {@code containsKey}, and two methods over one
 * concept would have to be kept in step forever.
 */
public interface MediaAssetBatchQuery {

    Map<UUID, MediaAsset> findAssetsByIds(UUID tourOperatorId, Set<UUID> mediaIds);

    /**
     * What a rendered {@code <img>} needs: where the bytes are, and the three
     * things that describe them.
     *
     * <p><b>{@code alt}, {@code width} and {@code height} are null on every row
     * today</b> and that is expected rather than broken — the columns exist
     * (media V1) and nothing populates them yet. Both write paths are their own
     * work: alt cannot be derived, and measuring dimensions needs a port, since
     * {@code javax.imageio} is not {@code java.*}.
     *
     * @param storageKey bucket-relative, never a URL (PATTERNS §5)
     */
    record MediaAsset(String storageKey, String alt, Integer width, Integer height) {}
}
