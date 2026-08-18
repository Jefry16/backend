package com.vointika.touroperator.application.usecase;

import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.touroperator.domain.entity.TourOperatorInvitation;
import com.vointika.touroperator.domain.repository.TourOperatorInvitationRepository;

import java.util.Map;
import java.util.UUID;

/**
 * Cancels a pending team invitation (PENDING → REVOKED); the emailed accept link
 * stops working. Membership on the operator is enforced by the route interceptor;
 * this adds the role gate — only an ADMIN+ may revoke (mirrors invite).
 *
 * <p>Guards: caller not ADMIN+ → 403; the invitation isn't under this operator
 * → 404 (a cross-tenant id resolves empty); already accepted or revoked → 409
 * (only pending invitations can be revoked — an accepted invite is undone via
 * member removal, not here). No email; the {@code invitation.revoked} audit
 * entry rides the save's transaction.
 */
public class RevokeInvitationUseCase {

    private final TourOperatorInvitationRepository invitationRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public RevokeInvitationUseCase(TourOperatorInvitationRepository invitationRepository,
                                   TourOperatorMembershipCheck membershipCheck,
                                   TransactionRunner transactionRunner,
                                   AuditTrailPort auditTrailPort) {
        this.invitationRepository = invitationRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID invitationId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        TourOperatorInvitation invitation = invitationRepository
                .findByIdAndTourOperatorId(invitationId, tourOperatorId)
                .orElseThrow(TourOperatorInvitationRepository.NOT_FOUND);
        invitation.revoke();
        transactionRunner.run(() -> {
            invitationRepository.save(invitation);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "INVITATION", invitationId, "invitation.revoked",
                    Map.of("email", invitation.getEmail().value())));
        });
    }
}
