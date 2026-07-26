package com.vointika.media.application.usecase;

import com.vointika.media.application.port.MediaStoragePort;
import com.vointika.media.domain.entity.Media;
import com.vointika.media.domain.repository.MediaRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

/**
 * Deletes a media record. ADMIN+ only (mirrors upload); membership enforced by
 * the interceptor. Guards: caller not ADMIN+ → 403; the id isn't under this
 * operator → 404.
 *
 * <p>The row is removed first, then the object is deleted best-effort — an
 * orphaned object (delete succeeded, object-delete failed) is harmless and
 * swept out of band, whereas a live row pointing at a deleted object is not.
 * The row delete + audit entry share one transaction; the storage delete stays
 * after — it can't roll back.
 */
public class DeleteMediaUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteMediaUseCase.class);

    private final MediaRepository mediaRepository;
    private final MediaStoragePort mediaStoragePort;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public DeleteMediaUseCase(MediaRepository mediaRepository,
                              MediaStoragePort mediaStoragePort,
                              TourOperatorMembershipCheck membershipCheck,
                              TransactionRunner transactionRunner,
                              AuditTrailPort auditTrailPort) {
        this.mediaRepository = mediaRepository;
        this.mediaStoragePort = mediaStoragePort;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID mediaId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        Media media = mediaRepository.findByIdAndTourOperatorId(mediaId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found"));

        transactionRunner.run(() -> {
            mediaRepository.deleteByIdAndTourOperatorId(mediaId, tourOperatorId);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "MEDIA", mediaId, "media.deleted",
                    Map.of("fileName", media.getOriginalName())));
        });
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
