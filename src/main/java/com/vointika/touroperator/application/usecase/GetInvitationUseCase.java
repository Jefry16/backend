package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.touroperator.application.dto.output.InvitationView;
import com.vointika.touroperator.domain.entity.TourOperatorInvitation;
import com.vointika.touroperator.domain.repository.TourOperatorInvitationRepository;

import java.time.Instant;
import java.util.UUID;

/**
 * Reads a single invitation for the admin side. Membership on the operator is
 * enforced by the route interceptor; this adds the role gate — only an ADMIN+
 * may read invitations (mirrors invite/resend/revoke, keeping the invitation a
 * uniformly ADMIN+ resource).
 *
 * <p>Guards: caller not ADMIN+ → 403; the invitation isn't under this operator
 * → 404 (a cross-tenant id resolves empty). Returns the raw lifecycle status
 * plus a server-computed {@code expired} flag.
 */
public class GetInvitationUseCase {

    private final TourOperatorInvitationRepository invitationRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetInvitationUseCase(TourOperatorInvitationRepository invitationRepository,
                                TourOperatorMembershipCheck membershipCheck) {
        this.invitationRepository = invitationRepository;
        this.membershipCheck = membershipCheck;
    }

    public InvitationView execute(UUID tourOperatorId, UUID invitationId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        TourOperatorInvitation invitation = invitationRepository
                .findByIdAndTourOperatorId(invitationId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
        return InvitationView.from(invitation, Instant.now());
    }
}
