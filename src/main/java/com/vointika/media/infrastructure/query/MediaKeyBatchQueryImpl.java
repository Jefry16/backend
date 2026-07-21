package com.vointika.media.infrastructure.query;

import com.vointika.media.domain.repository.MediaRepository;
import com.vointika.shared.port.MediaKeyBatchQuery;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The media context's adapter for the shared {@link MediaKeyBatchQuery} seam:
 * resolves media ids to storage keys, tenant-scoped to the operator (ids not
 * owned by the operator are absent from the result).
 */
@Component
public class MediaKeyBatchQueryImpl implements MediaKeyBatchQuery {

    private final MediaRepository mediaRepository;

    public MediaKeyBatchQueryImpl(MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    @Override
    public Map<UUID, String> findKeysByIds(UUID tourOperatorId, Set<UUID> mediaIds) {
        return mediaRepository.findKeysByTourOperatorIdAndIds(tourOperatorId, mediaIds);
    }
}
