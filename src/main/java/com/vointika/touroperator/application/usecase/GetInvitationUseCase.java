package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.port.UserContactView;
import com.vointika.touroperator.application.dto.output.InvitationView;
import com.vointika.touroperator.domain.entity.TourOperatorInvitation;
import com.vointika.touroperator.domain.repository.TourOperatorInvitationRepository;

import java.time.Instant;
import java.util.UUID;

/**
 * Reads a single invitation. Visible to ANY member of the operator (STAFF
 * included) — read-only visibility; the mutating actions (invite / resend /
 * revoke) stay ADMIN+. Membership is enforced by the route interceptor and
 * re-asserted here (defense-in-depth, mirroring the members roster).
 *
 * <p>Guards: caller not a member → 404 (indistinguishable from a missing
 * operator, tenant isolation); the invitation isn't under this operator → 404
 * (a cross-tenant id resolves empty). Returns the raw lifecycle status plus a
 * server-computed {@code expired} flag.
 */
public class GetInvitationUseCase {

    private final TourOperatorInvitationRepository invitationRepository;
    private final UserAccountQuery userAccountQuery;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetInvitationUseCase(TourOperatorInvitationRepository invitationRepository,
                                UserAccountQuery userAccountQuery,
                                TourOperatorMembershipCheck membershipCheck) {
        this.invitationRepository = invitationRepository;
        this.userAccountQuery = userAccountQuery;
        this.membershipCheck = membershipCheck;
    }

    public InvitationView execute(UUID tourOperatorId, UUID invitationId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        TourOperatorInvitation invitation = invitationRepository
                .findByIdAndTourOperatorId(invitationId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
        // Best-effort inviter name (id is always present); null if unresolvable.
        String invitedByName = userAccountQuery.findContact(invitation.getInvitedByUserId())
                .map(UserContactView::name).orElse(null);
        return InvitationView.from(invitation, Instant.now(), invitedByName);
    }
}
