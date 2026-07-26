package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.touroperator.domain.entity.TourOperatorMember;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;

import java.util.Map;
import java.util.UUID;

/**
 * Removes a team member for {@code DELETE /api/tour-operators/{id}/members/{userId}}
 * — two paths sharing the last-OWNER invariant:
 *
 * <ul>
 *   <li><b>Self ({@code userId == caller}) = leave:</b> any member may leave, EXCEPT
 *       the last OWNER (409 — transfer first). No role gate beyond membership (the
 *       interceptor).</li>
 *   <li><b>Other:</b> caller must be ADMIN+. An ADMIN+ may remove any non-owner
 *       member (including a peer ADMIN); only an OWNER may remove an OWNER (else
 *       403), and the last OWNER is never removable (409).</li>
 * </ul>
 *
 */
public class RemoveTeamMemberUseCase {

    private final TourOperatorMemberRepository memberRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public RemoveTeamMemberUseCase(TourOperatorMemberRepository memberRepository,
                                   TourOperatorMembershipCheck membershipCheck,
                                   TransactionRunner transactionRunner,
                                   AuditTrailPort auditTrailPort) {
        this.memberRepository = memberRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID targetUserId, UUID callerUserId) {
        boolean isSelf = targetUserId.equals(callerUserId);
        if (!isSelf) {
            membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        }

        transactionRunner.run(() -> {
            // Full member read (not role-only): the removed member's name goes to
            // the trail's details — after the delete, that's where it survives.
            TourOperatorMember member = memberRepository
                    .findByTourOperatorIdAndUserId(tourOperatorId, targetUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
            MemberRole targetRole = member.getRole();

            if (targetRole == MemberRole.OWNER) {
                if (!isSelf) {
                    // Only an owner may act on an owner.
                    MemberRole callerRole = memberRepository
                            .findRoleByTourOperatorIdAndUserId(tourOperatorId, callerUserId)
                            .orElse(null);
                    if (callerRole != MemberRole.OWNER) {
                        throw new ForbiddenException("Only the owner can remove the owner");
                    }
                }
                if (memberRepository.countByTourOperatorIdAndRole(tourOperatorId, MemberRole.OWNER) <= 1) {
                    throw new ConflictException(
                            isSelf
                                    ? "Transfer ownership to another member before leaving"
                                    : "Transfer ownership to another member before removing the owner");
                }
            }

            memberRepository.deleteByTourOperatorIdAndUserId(tourOperatorId, targetUserId);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "MEMBER", targetUserId, "member.removed",
                    Map.of("memberName", member.getName())));
        });
    }
}
