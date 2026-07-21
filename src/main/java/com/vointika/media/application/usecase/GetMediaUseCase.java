package com.vointika.media.application.usecase;

import com.vointika.media.application.dto.output.MediaView;
import com.vointika.media.domain.entity.Media;
import com.vointika.media.domain.repository.MediaRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.port.UserContactView;

import java.util.UUID;

/**
 * Reads a single media record. <b>Any member</b> may view (read-only; upload and
 * delete stay ADMIN+). Membership is enforced by the interceptor and re-asserted
 * here. Guards: caller not a member → 404; the id isn't under this operator → 404
 * (a cross-tenant id resolves empty).
 */
public class GetMediaUseCase {

    private final MediaRepository mediaRepository;
    private final UserAccountQuery userAccountQuery;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetMediaUseCase(MediaRepository mediaRepository,
                           UserAccountQuery userAccountQuery,
                           TourOperatorMembershipCheck membershipCheck) {
        this.mediaRepository = mediaRepository;
        this.userAccountQuery = userAccountQuery;
        this.membershipCheck = membershipCheck;
    }

    public MediaView execute(UUID tourOperatorId, UUID mediaId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        Media media = mediaRepository.findByIdAndTourOperatorId(mediaId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found"));
        String uploaderName = userAccountQuery.findContact(media.getCreatedBy())
                .map(UserContactView::name).orElse(null);
        return MediaView.from(media, uploaderName);
    }
}
