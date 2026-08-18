package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.FieldChange;
import com.vointika.touroperator.domain.entity.TourOperatorMember;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;
import com.vointika.shared.exception.UniqueConstraintViolationException;

import java.util.List;
import java.util.Map;
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
 *       owner always remains — recorded as ONE {@code ownership.transferred}
 *       entry (the demotion rides it in details, no second entry).</li>
 *   <li><b>To ADMIN/STAFF:</b> an ADMIN+ may re-tier any non-owner. The OWNER can
 *       only be acted on by an OWNER (else 403); demoting the last OWNER is refused
 *       (409, transfer first). Same-role is a no-op.</li>
 * </ul>
 */
public class ChangeMemberRoleUseCase {

    /** Published: the 403 inside a permission an admin otherwise has. */
    public static final String OWNER_ONLY =
            "Only the owner can change the owner's role";

    private final TourOperatorMemberRepository memberRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public ChangeMemberRoleUseCase(TourOperatorMemberRepository memberRepository,
                                   TourOperatorMembershipCheck membershipCheck,
                                   TransactionRunner transactionRunner,
                                   AuditTrailPort auditTrailPort) {
        this.memberRepository = memberRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
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
        } catch (UniqueConstraintViolationException e) {
            // A concurrent ownership transfer won the single-OWNER partial unique
            // index race; the stale team snapshot this tx read is no longer valid.
            throw new ConflictException("The team's ownership just changed — reload and try again");
        }
    }

    private void apply(UUID tourOperatorId, UUID targetUserId, MemberRole newRole, UUID callerUserId) {
        // Targeted reads (no full-team load): the target member, the owner count,
        // and — only when needed — the caller's role/entity.
        TourOperatorMember target = memberRepository.findByTourOperatorIdAndUserId(tourOperatorId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (targetUserId.equals(callerUserId)) {
            boolean lastOwner = target.getRole() == MemberRole.OWNER && soleOwner(tourOperatorId);
            throw new ConflictException(lastOwner
                    ? "Transfer ownership to another member before changing your own role"
                    : "You cannot change your own role");
        }

        if (newRole == MemberRole.OWNER) {
            transferOwnership(tourOperatorId, target, callerUserId);
            return;
        }
        MemberRole roleBefore = target.getRole();

        // Demotion / lateral to ADMIN or STAFF.
        if (target.getRole() == MemberRole.OWNER) {
            MemberRole callerRole = memberRepository
                    .findRoleByTourOperatorIdAndUserId(tourOperatorId, callerUserId).orElse(null);
            if (callerRole != MemberRole.OWNER) {
                throw new ForbiddenException(OWNER_ONLY);
            }
            if (soleOwner(tourOperatorId)) {
                throw new ConflictException(
                        "Transfer ownership to another member before demoting the owner");
            }
        }
        if (target.getRole() == newRole) {
            return; // no-op — records nothing
        }
        target.changeRole(newRole);
        memberRepository.save(target);
        auditTrailPort.append(new NewAuditEntry(
                tourOperatorId, AuditActor.user(callerUserId),
                "MEMBER", targetUserId, "member.role_changed",
                Map.of("memberName", target.getName()),
                List.of(new FieldChange("role", roleBefore.name(), newRole.name()))));
    }

    /** Promote {@code target} to OWNER and demote the acting owner to ADMIN, atomically. */
    private void transferOwnership(UUID tourOperatorId, TourOperatorMember target, UUID callerUserId) {
        TourOperatorMember caller = memberRepository.findByTourOperatorIdAndUserId(tourOperatorId, callerUserId)
                .orElseThrow(() -> new ForbiddenException("Only the owner can transfer ownership"));
        MemberRole targetRoleBefore = target.getRole();
        caller.changeRole(MemberRole.ADMIN);
        target.changeRole(MemberRole.OWNER);
        // The repo flushes the demotion before the promotion so the single-owner
        // partial unique index never momentarily sees two OWNERs.
        memberRepository.transferOwnership(caller, target);
        // ONE entry for the atomic swap — the acting owner's demotion rides it.
        auditTrailPort.append(new NewAuditEntry(
                tourOperatorId, AuditActor.user(callerUserId),
                "MEMBER", target.getUserId(), "ownership.transferred",
                Map.of("memberName", target.getName(), "demotedUserId", callerUserId.toString()),
                List.of(new FieldChange("role", targetRoleBefore.name(), MemberRole.OWNER.name()))));
    }

    private boolean soleOwner(UUID tourOperatorId) {
        return memberRepository.countByTourOperatorIdAndRole(tourOperatorId, MemberRole.OWNER) <= 1;
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
