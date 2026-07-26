package com.vointika.touroperator.application.usecase;

import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.touroperator.domain.entity.TourOperatorMember;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChangeMemberRoleUseCaseTest {

    private final AuditTrailPort auditTrailPort = mock(AuditTrailPort.class);

    private TourOperatorMemberRepository memberRepository;
    private TourOperatorMembershipCheck membershipCheck;
    private ChangeMemberRoleUseCase useCase;

    private final UUID op = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID staffId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        memberRepository = mock(TourOperatorMemberRepository.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        TransactionRunner tx = new TransactionRunner() {
            @Override public <T> T call(Supplier<T> work) { return work.get(); }
            @Override public void run(Runnable work) { work.run(); }
        };
        useCase = new ChangeMemberRoleUseCase(memberRepository, membershipCheck, tx, auditTrailPort);
        when(memberRepository.save(any())).thenAnswer(a -> a.getArgument(0));
    }

    private TourOperatorMember member(UUID userId, MemberRole role) {
        return new TourOperatorMember(UUID.randomUUID(), op, userId, role, false, Instant.now(),
                "Test User", "test@example.test");
    }

    /** Stubs the targeted reads the use case makes: per-user member/role lookups + the OWNER count. */
    private void team(TourOperatorMember... members) {
        when(memberRepository.findByTourOperatorIdAndUserId(eq(op), any())).thenReturn(Optional.empty());
        when(memberRepository.findRoleByTourOperatorIdAndUserId(eq(op), any())).thenReturn(Optional.empty());
        long owners = 0;
        for (TourOperatorMember m : members) {
            when(memberRepository.findByTourOperatorIdAndUserId(op, m.getUserId())).thenReturn(Optional.of(m));
            when(memberRepository.findRoleByTourOperatorIdAndUserId(op, m.getUserId())).thenReturn(Optional.of(m.getRole()));
            if (m.getRole() == MemberRole.OWNER) {
                owners++;
            }
        }
        when(memberRepository.countByTourOperatorIdAndRole(op, MemberRole.OWNER)).thenReturn(owners);
    }

    @Test
    void cannotChangeYourOwnRole() {
        team(member(adminId, MemberRole.ADMIN));
        assertThrows(ConflictException.class, () -> useCase.execute(op, adminId, "STAFF", adminId));
        verify(memberRepository, never()).save(any());
    }

    @Test
    void adminMayRetierAPeerAdminToStaff_permissive() {
        UUID peerAdmin = UUID.randomUUID();
        team(member(ownerId, MemberRole.OWNER), member(adminId, MemberRole.ADMIN),
                member(peerAdmin, MemberRole.ADMIN));

        useCase.execute(op, peerAdmin, "STAFF", adminId);

        ArgumentCaptor<TourOperatorMember> c = ArgumentCaptor.forClass(TourOperatorMember.class);
        verify(memberRepository).save(c.capture());
        assertEquals(MemberRole.STAFF, c.getValue().getRole());
    }

    @Test
    void adminCannotGrantOwner_noEscalation() {
        // ensureOwner is the gate for granting OWNER; an ADMIN fails it → 403.
        doThrow(new ForbiddenException("owner only")).when(membershipCheck).ensureOwner(eq(adminId), eq(op));
        assertThrows(ForbiddenException.class, () -> useCase.execute(op, staffId, "OWNER", adminId));
    }

    @Test
    void ownerTransfersOwnershipAtomically() {
        team(member(ownerId, MemberRole.OWNER), member(adminId, MemberRole.ADMIN));

        useCase.execute(op, adminId, "OWNER", ownerId);

        ArgumentCaptor<TourOperatorMember> demoted = ArgumentCaptor.forClass(TourOperatorMember.class);
        ArgumentCaptor<TourOperatorMember> promoted = ArgumentCaptor.forClass(TourOperatorMember.class);
        verify(memberRepository).transferOwnership(demoted.capture(), promoted.capture());
        assertEquals(ownerId, demoted.getValue().getUserId());
        assertEquals(MemberRole.ADMIN, demoted.getValue().getRole());
        assertEquals(adminId, promoted.getValue().getUserId());
        assertEquals(MemberRole.OWNER, promoted.getValue().getRole());
    }

    @Test
    void nonOwnerCannotDemoteTheOwner() {
        // Two owners is impossible, so construct: caller is ADMIN, target is OWNER.
        team(member(ownerId, MemberRole.OWNER), member(adminId, MemberRole.ADMIN));
        assertThrows(ForbiddenException.class, () -> useCase.execute(op, ownerId, "STAFF", adminId));
    }

    @Test
    void theLastOwnerCannotSelfDemoteWithoutTransferring() {
        // The only reachable last-owner-demote path: the sole owner tries to change
        // their own role. Blocked with the transfer-first conflict (409).
        team(member(ownerId, MemberRole.OWNER), member(adminId, MemberRole.ADMIN));
        assertThrows(ConflictException.class, () -> useCase.execute(op, ownerId, "ADMIN", ownerId));
    }

    @Test
    void unknownRoleIs422() {
        team(member(adminId, MemberRole.ADMIN), member(staffId, MemberRole.STAFF));
        assertThrows(InvalidFieldException.class, () -> useCase.execute(op, staffId, "SUPERUSER", adminId));
    }

    @Test
    void missingTargetIs404() {
        team(member(adminId, MemberRole.ADMIN));
        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(op, UUID.randomUUID(), "STAFF", adminId));
    }

    @Test
    void sameRoleIsANoOp() {
        team(member(adminId, MemberRole.ADMIN), member(staffId, MemberRole.STAFF));
        useCase.execute(op, staffId, "STAFF", adminId);
        verify(memberRepository, never()).save(any());
    }
}
