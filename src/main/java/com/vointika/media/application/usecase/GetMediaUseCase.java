package com.vointika.media.application.usecase;

import com.vointika.media.application.dto.output.MediaView;
import com.vointika.media.domain.entity.Media;
import com.vointika.media.domain.repository.MediaRepository;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/**
 * Reads a single media record. <b>Any member</b> may view (read-only; upload and
 * delete stay ADMIN+). Membership is enforced by the interceptor and re-asserted
 * here. Guards: caller not a member → 404; the id isn't under this operator → 404
 * (a cross-tenant id resolves empty).
 */
public class GetMediaUseCase {

    private final MediaRepository mediaRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetMediaUseCase(MediaRepository mediaRepository,
                           TourOperatorMembershipCheck membershipCheck) {
        this.mediaRepository = mediaRepository;
        this.membershipCheck = membershipCheck;
    }

    public MediaView execute(UUID tourOperatorId, UUID mediaId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        Media media = mediaRepository.requireByIdAndTourOperatorId(mediaId, tourOperatorId);
        // Uploader name is snapshotted on the row — no identity lookup.
        return MediaView.from(media);
    }
}
