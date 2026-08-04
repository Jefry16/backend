package com.vointika.media.infrastructure.query;

import com.vointika.media.domain.repository.MediaRepository;
import com.vointika.shared.port.MediaAssetBatchQuery;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The media context's adapter for the shared {@link MediaAssetBatchQuery} seam:
 * resolves media ids to the stored asset, tenant-scoped to the operator (ids not
 * owned by the operator are absent from the result).
 */
@Component
public class MediaAssetBatchQueryImpl implements MediaAssetBatchQuery {

    private final MediaRepository mediaRepository;

    public MediaAssetBatchQueryImpl(MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    @Override
    public Map<UUID, MediaAsset> findAssetsByIds(UUID tourOperatorId, Set<UUID> mediaIds) {
        return mediaRepository.findAssetsByTourOperatorIdAndIds(tourOperatorId, mediaIds);
    }
}
