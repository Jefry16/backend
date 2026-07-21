package com.vointika.experience.application.service;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.port.MediaKeyBatchQuery;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Validates that every media id an experience references (gallery + thumbnail)
 * belongs to the operator's own media library, through the tenant-scoped
 * {@link MediaKeyBatchQuery} seam. A foreign or nonexistent id is absent from
 * the result, so a returned-count shortfall means at least one bad id → 422.
 * The seam is why media ids can be plain UUID references (no cross-context FK).
 */
public class MediaReferenceValidator {

    private final MediaKeyBatchQuery mediaKeyBatchQuery;

    public MediaReferenceValidator(MediaKeyBatchQuery mediaKeyBatchQuery) {
        this.mediaKeyBatchQuery = mediaKeyBatchQuery;
    }

    public void validate(UUID tourOperatorId, List<UUID> mediaIds, UUID thumbnailMediaId) {
        Set<UUID> ids = new HashSet<>();
        if (mediaIds != null) {
            ids.addAll(mediaIds);
        }
        if (thumbnailMediaId != null) {
            ids.add(thumbnailMediaId);
        }
        if (ids.isEmpty()) {
            return;
        }
        int owned = mediaKeyBatchQuery.findKeysByIds(tourOperatorId, ids).size();
        if (owned < ids.size()) {
            throw new InvalidFieldException("One or more media ids are not in this operator's library");
        }
    }
}
