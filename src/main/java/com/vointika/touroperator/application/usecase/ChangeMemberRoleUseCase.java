package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.touroperator.domain.entity.TourOperatorMember;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Changes a member's role — including ownership transfer — for
 * {@code PATCH /api/tour-operators/{id}/members/{userId}}. The permissive,
 * no-privilege-escalation rule: you can grant at most your own tier, so an ADMIN
 * may re-tier any non-owner (including a peer ADMIN) but can never mint an OWNER.
 *
 * <ul>
 *   <li>Caller must be ADMIN+; you can never change your OWN role (409).</li>
 *   <li><b>To OWNER = ownership transfer:</b> only an OWNER may (else 403); the
 *       acting owner is demoted to ADMIN in the SAME transaction, so exactly one
 *       owner always remains ({@code member.role_changed} audit is subtracted —
 *       no audit context yet).</li>
 *   <li><b>To ADMIN/STAFF:</b> an ADMIN+ may re-tier any non-owner. The OWNER can
 *       only be acted on by an OWNER (else 403); demoting the last OWNER is refused
 *       (409, transfer first). Same-role is a no-op.</li>
 * </ul>
 */
public class ChangeMemberRoleUseCase {

    private final TourOperatorMemberRepository memberRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;

    public ChangeMemberRoleUseCase(TourOperatorMemberRepository memberRepository,
                                   TourOperatorMembershipCheck membershipCheck,
                                   TransactionRunner transactionRunner) {
        this.memberRepository = memberRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
    }

    public void execute(UUID tourOperatorId, UUID targetUserId, String rawRole, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        MemberRole newRole = parseRole(rawRole);
        if (newRole == MemberRole.OWNER) {
            // Only an owner may transfer ownership — the no-escalation guarantee
            // (an ADMIN can never mint an OWNER).
            membershipCheck.ensureOwner(callerUserId, tourOperatorId);
        }

        try {
            transactionRunner.run(() -> apply(tourOperatorId, targetUserId, newRole, callerUserId));
        } catch (DataIntegrityViolationException e) {
            // A concurrent ownership transfer won the single-OWNER partial unique
            // index race; the stale team snapshot this tx read is no longer valid.
            throw new ConflictException("The team's ownership just changed — reload and try again");
        }
    }

    private void apply(UUID tourOperatorId, UUID targetUserId, MemberRole newRole, UUID callerUserId) {
        List<TourOperatorMember> team = memberRepository.findByTourOperatorId(tourOperatorId);
        TourOperatorMember target = find(team, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (targetUserId.equals(callerUserId)) {
            throw new ConflictException(
                    (target.getRole() == MemberRole.OWNER && ownerCount(team) <= 1)
                            ? "Transfer ownership to another member before changing your own role"
                            : "You cannot change your own role");
        }

        if (newRole == MemberRole.OWNER) {
            transferOwnership(team, target, callerUserId);
            return;
        }

        // Demotion / lateral to ADMIN or STAFF.
        if (target.getRole() == MemberRole.OWNER) {
            TourOperatorMember caller = find(team, callerUserId).orElseThrow();
            if (caller.getRole() != MemberRole.OWNER) {
                throw new ForbiddenException("Only the owner can change the owner's role");
            }
            if (ownerCount(team) <= 1) {
                throw new ConflictException(
                        "Transfer ownership to another member before demoting the owner");
            }
        }
        if (target.getRole() == newRole) {
            return; // no-op
        }
        target.changeRole(newRole);
        memberRepository.save(target);
    }

    /** Promote {@code target} to OWNER and demote the acting owner to ADMIN, atomically. */
    private void transferOwnership(List<TourOperatorMember> team, TourOperatorMember target, UUID callerUserId) {
        TourOperatorMember caller = find(team, callerUserId)
                .orElseThrow(() -> new ForbiddenException("Only the owner can transfer ownership"));
        caller.changeRole(MemberRole.ADMIN);
        target.changeRole(MemberRole.OWNER);
        // The repo flushes the demotion before the promotion so the single-owner
        // partial unique index never momentarily sees two OWNERs.
        memberRepository.transferOwnership(caller, target);
    }

    private static Optional<TourOperatorMember> find(List<TourOperatorMember> team, UUID userId) {
        return team.stream().filter(m -> m.getUserId().equals(userId)).findFirst();
    }

    private static long ownerCount(List<TourOperatorMember> team) {
        return team.stream().filter(m -> m.getRole() == MemberRole.OWNER).count();
    }

    private static MemberRole parseRole(String rawRole) {
        if (rawRole != null) {
            for (MemberRole candidate : MemberRole.values()) {
                if (candidate.name().equals(rawRole)) {
                    return candidate;
                }
            }
        }
        throw new InvalidFieldException("Role must be one of: OWNER, ADMIN, STAFF");
    }
}
