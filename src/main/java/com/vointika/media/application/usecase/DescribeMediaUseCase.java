package com.vointika.media.application.usecase;

import com.vointika.media.application.dto.input.DescribeMediaInput;
import com.vointika.media.domain.entity.Media;
import com.vointika.media.domain.repository.MediaRepository;
import com.vointika.media.domain.valueobject.MediaAlt;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;

import java.util.Map;
import java.util.UUID;

/**
 * Sets or clears an image's alt text. ADMIN+, like every other media mutation.
 *
 * <p>Alt is the one part of a media row that arrives <b>after</b> the upload:
 * width and height are measured from the bytes, but a description of what the
 * image shows exists only in the head of whoever chose it.
 *
 * <p>A <b>blank</b> alt clears it, the convention every optional field in this
 * API uses. Null and empty are not the same thing to a screen reader — a missing
 * alt is an undescribed image, while {@code alt=""} declares it decorative — but
 * the column holds only one absence, so blank means "no description".
 *
 * <p>The lookup is tenant-scoped: a media id from another operator is a 404.
 */
public class DescribeMediaUseCase {

    private final MediaRepository mediaRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public DescribeMediaUseCase(MediaRepository mediaRepository,
                                TourOperatorMembershipCheck membershipCheck,
                                TransactionRunner transactionRunner,
                                AuditTrailPort auditTrailPort) {
        this.mediaRepository = mediaRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID mediaId,
                        DescribeMediaInput input, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        Media media = mediaRepository.requireByIdAndTourOperatorId(mediaId, tourOperatorId);

        MediaAlt alt = input.alt() == null || input.alt().isBlank()
                ? null : new MediaAlt(input.alt());
        String before = media.getAlt() == null ? null : media.getAlt().value();
        String after = alt == null ? null : alt.value();
        if (java.util.Objects.equals(before, after)) {
            return;
        }

        media.describe(alt);
        transactionRunner.run(() -> {
            mediaRepository.save(media);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "MEDIA", mediaId, "media.described",
                    Map.of("alt", after == null ? "" : after)));
        });
    }
}
