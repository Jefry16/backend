package com.vointika.touroperator.application.usecase;

import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.touroperator.domain.entity.TourOperatorMember;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RemoveTeamMemberUseCaseTest {

    private final AuditTrailPort auditTrailPort = mock(AuditTrailPort.class);

    private TourOperatorMemberRepository memberRepository;
    private TourOperatorMembershipCheck membershipCheck;
    private RemoveTeamMemberUseCase useCase;

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
        useCase = new RemoveTeamMemberUseCase(memberRepository, membershipCheck, tx, auditTrailPort);
    }

    // The use case reads the TARGET as a full member (its name goes to the
    // audit details) but still reads the CALLER role-only — stub both.
    private void roleOf(UUID userId, MemberRole role) {
        when(memberRepository.findRoleByTourOperatorIdAndUserId(op, userId))
                .thenReturn(Optional.ofNullable(role));
        when(memberRepository.findByTourOperatorIdAndUserId(op, userId))
                .thenReturn(role == null
                        ? Optional.empty()
                        : Optional.of(new TourOperatorMember(
                                UUID.randomUUID(), op, userId, role, false,
                                "Member " + role, role.name().toLowerCase() + "@x.test")));
    }
    private void owners(long n) {
        when(memberRepository.countByTourOperatorIdAndRole(op, MemberRole.OWNER)).thenReturn(n);
    }

    @Test
    void staffMayLeave() {
        roleOf(staffId, MemberRole.STAFF);
        useCase.execute(op, staffId, staffId); // self
        verify(memberRepository).deleteByTourOperatorIdAndUserId(op, staffId);
    }

    @Test
    void theLastOwnerCannotLeave() {
        roleOf(ownerId, MemberRole.OWNER);
        owners(1);
        assertThrows(ConflictException.class, () -> useCase.execute(op, ownerId, ownerId));
        verify(memberRepository, never()).deleteByTourOperatorIdAndUserId(any(), any());
    }

    @Test
    void adminMayRemoveStaff() {
        roleOf(staffId, MemberRole.STAFF);
        useCase.execute(op, staffId, adminId); // admin removes staff
        verify(membershipCheck).ensureAdmin(adminId, op);
        verify(memberRepository).deleteByTourOperatorIdAndUserId(op, staffId);
    }

    @Test
    void aStaffCallerRemovingAnotherIsForbidden() {
        doThrow(new ForbiddenException("admin only")).when(membershipCheck).ensureAdmin(eq(staffId), eq(op));
        assertThrows(ForbiddenException.class, () -> useCase.execute(op, adminId, staffId));
    }

    @Test
    void onlyAnOwnerMayRemoveTheOwner() {
        roleOf(ownerId, MemberRole.OWNER);
        roleOf(adminId, MemberRole.ADMIN);
        owners(1);
        assertThrows(ForbiddenException.class, () -> useCase.execute(op, ownerId, adminId));
    }

    @Test
    void missingMemberIs404() {
        roleOf(staffId, null);
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(op, staffId, adminId));
    }
}
