package com.vointika.media.application.usecase;

import com.vointika.media.application.port.MediaStoragePort;
import com.vointika.media.domain.entity.Media;
import com.vointika.media.domain.repository.MediaRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Deletes a media record. ADMIN+ only (mirrors upload); membership enforced by
 * the interceptor. Guards: caller not ADMIN+ → 403; the id isn't under this
 * operator → 404.
 *
 * <p>The row is removed first, then the object is deleted best-effort — an
 * orphaned object (delete succeeded, object-delete failed) is harmless and
 * swept out of band, whereas a live row pointing at a deleted object is not.
 * No audit entry (no audit context yet).
 */
public class DeleteMediaUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteMediaUseCase.class);

    private final MediaRepository mediaRepository;
    private final MediaStoragePort mediaStoragePort;
    private final TourOperatorMembershipCheck membershipCheck;

    public DeleteMediaUseCase(MediaRepository mediaRepository,
                              MediaStoragePort mediaStoragePort,
                              TourOperatorMembershipCheck membershipCheck) {
        this.mediaRepository = mediaRepository;
        this.mediaStoragePort = mediaStoragePort;
        this.membershipCheck = membershipCheck;
    }

    public void execute(UUID tourOperatorId, UUID mediaId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        Media media = mediaRepository.findByIdAndTourOperatorId(mediaId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found"));

        mediaRepository.deleteByIdAndTourOperatorId(mediaId, tourOperatorId);
        deleteQuietly(media.getStorageKey());
    }

    private void deleteQuietly(String key) {
        try {
            mediaStoragePort.deleteObject(key);
        } catch (RuntimeException e) {
            log.warn("Failed to delete media object {} after row removal", key, e);
        }
    }
}
