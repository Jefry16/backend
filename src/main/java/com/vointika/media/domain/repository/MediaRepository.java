package com.vointika.media.domain.repository;

import com.vointika.media.domain.entity.Media;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.port.MediaAssetBatchQuery.MediaAsset;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface MediaRepository {

    Media save(Media media);

    /** Tenant-scoped lookup — an id under a different operator resolves empty. */
    Optional<Media> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    /** The operator's media library, cursor-paginated + filtered. */
    CursorPage<Media> list(ListQuery query);

    /** Removes the row if it belongs to this operator; returns whether a row was deleted. */
    boolean deleteByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    /**
     * id → the stored asset for the given ids that belong to this operator. Ids
     * not owned by the operator are absent from the map — backs the tenant-scoped
     * cross-context resolver.
     */
    Map<UUID, MediaAsset> findAssetsByTourOperatorIdAndIds(UUID tourOperatorId, Set<UUID> ids);
}
