package com.vointika.touroperator.application.policy;

import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TourOperatorMembershipPolicyTest {

    private TourOperatorMemberRepository memberRepository;
    private TourOperatorMembershipPolicy policy;

    private final UUID userId = UUID.randomUUID();
    private final UUID operatorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        memberRepository = mock(TourOperatorMemberRepository.class);
        policy = new TourOperatorMembershipPolicy(memberRepository);
    }

    private void role(MemberRole role) {
        when(memberRepository.existsByTourOperatorIdAndUserId(operatorId, userId)).thenReturn(role != null);
        when(memberRepository.findRoleByTourOperatorIdAndUserId(operatorId, userId))
                .thenReturn(Optional.ofNullable(role));
    }

    @Test
    void ensureMemberPassesForAMemberAnd404sForANonMember() {
        role(MemberRole.STAFF);
        assertDoesNotThrow(() -> policy.ensureMember(userId, operatorId));

        role(null);
        assertThrows(ResourceNotFoundException.class, () -> policy.ensureMember(userId, operatorId));
        assertThrows(ResourceNotFoundException.class, () -> policy.ensureMember(null, operatorId));
    }

    @Test
    void ensureAdminAllowsOwnerAndAdminButForbidsStaff() {
        role(MemberRole.OWNER);
        assertDoesNotThrow(() -> policy.ensureAdmin(userId, operatorId));
        role(MemberRole.ADMIN);
        assertDoesNotThrow(() -> policy.ensureAdmin(userId, operatorId));
        role(MemberRole.STAFF);
        assertThrows(ForbiddenException.class, () -> policy.ensureAdmin(userId, operatorId));
        role(null);
        assertThrows(ForbiddenException.class, () -> policy.ensureAdmin(userId, operatorId));
    }

    @Test
    void ensureOwnerAllowsOnlyOwner() {
        role(MemberRole.OWNER);
        assertDoesNotThrow(() -> policy.ensureOwner(userId, operatorId));
        role(MemberRole.ADMIN);
        assertThrows(ForbiddenException.class, () -> policy.ensureOwner(userId, operatorId));
    }
}
