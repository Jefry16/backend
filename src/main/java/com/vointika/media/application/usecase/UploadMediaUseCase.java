package com.vointika.media.application.usecase;

import com.vointika.media.application.port.MediaStoragePort;
import com.vointika.media.domain.entity.Media;
import com.vointika.media.domain.repository.MediaRepository;
import com.vointika.media.domain.valueobject.ContentType;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.port.UserContactView;
import com.vointika.shared.service.IdGenerator;
import com.vointika.shared.valueobject.AuditActor;

import com.vointika.media.application.port.ImageDimensionsPort;

import java.io.InputStream;
import java.util.function.Supplier;
import java.util.Map;
import java.util.UUID;

/**
 * Uploads a file to the operator's media library. Membership on the operator is
 * enforced by the route interceptor; this adds the role gate — only an ADMIN+
 * may upload.
 *
 * <p>Guards: caller not ADMIN+ → 403; content type not in the allowlist → 422;
 * empty or oversized file → 422. The object is streamed to storage <b>before</b>
 * the row is saved — a failed save strands an object (cleaned up out of band),
 * but a row never exists without its object. The row save + audit entry share
 * one transaction; the storage write stays outside — it can't roll back.
 */
public class UploadMediaUseCase {

    /** Images/PDF cap. Video (a larger tier) is deferred until a consumer needs it. */
    static final long MAX_BYTES = 25L * 1024 * 1024;

    private final MediaRepository mediaRepository;
    private final MediaStoragePort mediaStoragePort;
    private final ImageDimensionsPort imageDimensionsPort;
    private final TourOperatorMembershipCheck membershipCheck;
    private final UserAccountQuery userAccountQuery;
    private final IdGenerator idGenerator;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UploadMediaUseCase(MediaRepository mediaRepository,
                              MediaStoragePort mediaStoragePort,
                              ImageDimensionsPort imageDimensionsPort,
                              TourOperatorMembershipCheck membershipCheck,
                              UserAccountQuery userAccountQuery,
                              IdGenerator idGenerator,
                              TransactionRunner transactionRunner,
                              AuditTrailPort auditTrailPort) {
        this.mediaRepository = mediaRepository;
        this.mediaStoragePort = mediaStoragePort;
        this.imageDimensionsPort = imageDimensionsPort;
        this.membershipCheck = membershipCheck;
        this.userAccountQuery = userAccountQuery;
        this.idGenerator = idGenerator;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public UUID execute(UUID tourOperatorId, UUID callerUserId,
                        String rawContentType, long sizeBytes, String originalName,
                        Supplier<InputStream> body) {
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

        // Measured before storing, from its own stream. The container spools the
        // whole multipart before any handler runs (see application.yml), so asking
        // for the bytes twice re-reads the spool rather than buffering here — which
        // is why this takes a Supplier and not an InputStream.
        //
        // Empty is ordinary: a PDF has no dimensions, and a damaged header answers
        // the same way. It never fails the upload.
        var dimensions = imageDimensionsPort.measure(body.get(), contentType.value());

        // Store BEFORE persisting: a failed save strands an object, never a row
        // without its object. Storage can't roll back with the DB tx anyway.
        mediaStoragePort.putObject(key, contentType.value(), sizeBytes, body.get());

        String fileName = normalizeName(originalName, contentType);
        transactionRunner.run(() -> {
            mediaRepository.save(Media.upload(
                    id, tourOperatorId, key, contentType.value(), sizeBytes,
                    fileName, callerUserId, uploaderName,
                    dimensions.map(d -> d.width()).orElse(null),
                    dimensions.map(d -> d.height()).orElse(null)));
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "MEDIA", id, "media.uploaded",
                    Map.of("fileName", fileName, "contentType", contentType.value())));
        });
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
