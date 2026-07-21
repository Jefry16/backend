package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.touroperator.domain.entity.TourOperatorInvitation;
import com.vointika.touroperator.domain.repository.TourOperatorInvitationRepository;

import java.util.UUID;

/**
 * Cancels a pending team invitation (PENDING → REVOKED); the emailed accept link
 * stops working. Membership on the operator is enforced by the route interceptor;
 * this adds the role gate — only an ADMIN+ may revoke (mirrors invite).
 *
 * <p>Guards: caller not ADMIN+ → 403; the invitation isn't under this operator
 * → 404 (a cross-tenant id resolves empty); already accepted or revoked → 409
 * (only pending invitations can be revoked — an accepted invite is undone via
 * member removal, not here). No email, no audit entry (no audit context yet).
 */
public class RevokeInvitationUseCase {

    private final TourOperatorInvitationRepository invitationRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public RevokeInvitationUseCase(TourOperatorInvitationRepository invitationRepository,
                                   TourOperatorMembershipCheck membershipCheck) {
        this.invitationRepository = invitationRepository;
        this.membershipCheck = membershipCheck;
    }

    public void execute(UUID tourOperatorId, UUID invitationId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        TourOperatorInvitation invitation = invitationRepository
                .findByIdAndTourOperatorId(invitationId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
        invitation.revoke();
        invitationRepository.save(invitation);
    }
}
