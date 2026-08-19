package com.vointika.touroperator.application.policy;

import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    /**
     * <b>The one assertion that holds this sentence.</b> Collapsing it onto
     * {@link TourOperatorMembershipCheck#requiresRoleMessage} turned fifteen test copies
     * into fifteen calls to the method they assert, so every one of them holds for any
     * value — and a reword rewrites the published 403 of <b>eight</b> endpoints with the
     * suite green. That is strictly weaker than the copies it replaced, which is exactly
     * what `PATTERNS.md` §9a warns about; the collapse is only finished with this line.
     *
     * <p><b>Asserted against {@code MemberRole.ADMIN.name()}, not the bare string.</b>
     * The message is <em>built</em> from an argument: production passes
     * {@code minimum.name()} while all fifteen stubs hardcode {@code "ADMIN"}. Renaming
     * the enum constant would reword production while every stub kept agreeing with
     * itself. Pinning against the enum catches the rename as well as the reword.
     *
     * <p>It lives in {@code touroperator} because {@code MemberRole} does — the shared
     * port cannot see the enum, which is why the helper takes a name.
     */
    @Test
    void theInsufficientRoleMessageReadsThisWay() {
        assertEquals("This action requires ADMIN privileges",
                TourOperatorMembershipCheck.requiresRoleMessage(MemberRole.ADMIN.name()));
        assertEquals("This action requires OWNER privileges",
                TourOperatorMembershipCheck.requiresRoleMessage(MemberRole.OWNER.name()));
    }

    /**
     * And the policy really builds its refusal that way — the pin above would otherwise
     * hold while {@code requireAtLeast} threw something else entirely.
     */
    @Test
    void thePolicyThrowsTheMessageItPins() {
        role(MemberRole.STAFF);

        assertEquals(TourOperatorMembershipCheck.requiresRoleMessage(MemberRole.ADMIN.name()),
                assertThrows(ForbiddenException.class,
                        () -> policy.ensureAdmin(userId, operatorId)).getMessage());
    }

    @Test
    void ensureOwnerAllowsOnlyOwner() {
        role(MemberRole.OWNER);
        assertDoesNotThrow(() -> policy.ensureOwner(userId, operatorId));
        role(MemberRole.ADMIN);
        assertThrows(ForbiddenException.class, () -> policy.ensureOwner(userId, operatorId));
    }
}
