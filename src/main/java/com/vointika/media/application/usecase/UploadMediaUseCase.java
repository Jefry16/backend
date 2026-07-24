package com.vointika.media.application.usecase;

import com.vointika.media.application.port.MediaStoragePort;
import com.vointika.media.domain.entity.Media;
import com.vointika.media.domain.repository.MediaRepository;
import com.vointika.media.domain.valueobject.ContentType;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.port.UserContactView;
import com.vointika.shared.service.IdGenerator;

import java.io.InputStream;
import java.util.UUID;

/**
 * Uploads a file to the operator's media library. Membership on the operator is
 * enforced by the route interceptor; this adds the role gate — only an ADMIN+
 * may upload.
 *
 * <p>Guards: caller not ADMIN+ → 403; content type not in the allowlist → 422;
 * empty or oversized file → 422. The object is streamed to storage <b>before</b>
 * the row is saved — a failed save strands an object (cleaned up out of band),
 * but a row never exists without its object. No audit entry (no audit context yet).
 */
public class UploadMediaUseCase {

    /** Images/PDF cap. Video (a larger tier) is deferred until a consumer needs it. */
    static final long MAX_BYTES = 25L * 1024 * 1024;

    private final MediaRepository mediaRepository;
    private final MediaStoragePort mediaStoragePort;
    private final TourOperatorMembershipCheck membershipCheck;
    private final UserAccountQuery userAccountQuery;
    private final IdGenerator idGenerator;

    public UploadMediaUseCase(MediaRepository mediaRepository,
                              MediaStoragePort mediaStoragePort,
                              TourOperatorMembershipCheck membershipCheck,
                              UserAccountQuery userAccountQuery,
                              IdGenerator idGenerator) {
        this.mediaRepository = mediaRepository;
        this.mediaStoragePort = mediaStoragePort;
        this.membershipCheck = membershipCheck;
        this.userAccountQuery = userAccountQuery;
        this.idGenerator = idGenerator;
    }

    public UUID execute(UUID tourOperatorId, UUID callerUserId,
                        String rawContentType, long sizeBytes, String originalName, InputStream body) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        ContentType contentType = new ContentType(rawContentType);
        if (sizeBytes <= 0) {
            throw new InvalidFieldException("File is empty");
        }
        if (sizeBytes > MAX_BYTES) {
            throw new InvalidFieldException("File too large: max 25 MB");
        }

        // The uploader's name is snapshotted onto the row (so the library can sort
        // by who uploaded). The caller is the authenticated admin, so it resolves.
        String uploaderName = userAccountQuery.findContact(callerUserId)
                .map(UserContactView::name)
                .orElseThrow(() -> new ResourceNotFoundException("Uploading user not found"));

        UUID id = idGenerator.newId();
        String key = "tour-operators/" + tourOperatorId + "/" + id + "-" + sanitize(originalName, contentType);

        // Store BEFORE persisting: a failed save strands an object, never a row
        // without its object. Storage can't roll back with the DB tx anyway.
        mediaStoragePort.putObject(key, contentType.value(), sizeBytes, body);

        mediaRepository.save(Media.upload(
                id, tourOperatorId, key, contentType.value(), sizeBytes,
                normalizeName(originalName, contentType), callerUserId, uploaderName));
        return id;
    }

    /** Filesystem-safe key segment; falls back to {@code file.<ext>} when the name is empty. */
    private static String sanitize(String originalName, ContentType contentType) {
        String name = normalizeName(originalName, contentType);
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String normalizeName(String originalName, ContentType contentType) {
        if (originalName == null || originalName.isBlank()) {
            return "file." + contentType.extension();
        }
        return originalName.trim();
    }
}
