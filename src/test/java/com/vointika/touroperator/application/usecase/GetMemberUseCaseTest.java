package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.touroperator.application.dto.output.MemberListView;
import com.vointika.touroperator.domain.entity.TourOperatorMember;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetMemberUseCaseTest {

    private TourOperatorMemberRepository memberRepository;
    private TourOperatorMembershipCheck membershipCheck;
    private GetMemberUseCase useCase;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        memberRepository = mock(TourOperatorMemberRepository.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        useCase = new GetMemberUseCase(memberRepository, membershipCheck);
    }

    private TourOperatorMember member() {
        return new TourOperatorMember(
                UUID.randomUUID(), operatorId, userId, MemberRole.ADMIN, false,
                Instant.parse("2026-01-05T10:00:00Z"), "Grace Hopper", "grace@acme.test");
    }

    @Test
    void returnsTheMemberForAnyMember() {
        when(memberRepository.findByTourOperatorIdAndUserId(operatorId, userId))
                .thenReturn(Optional.of(member()));

        MemberListView view = useCase.execute(operatorId, userId, callerId);

        // Any member may view (STAFF included) — the gate is ensureMember.
        verify(membershipCheck).ensureMember(callerId, operatorId);
        assertEquals(userId, view.userId());
        assertEquals(MemberRole.ADMIN, view.role());
        assertEquals("Grace Hopper", view.name());
        assertEquals("grace@acme.test", view.email());
    }

    @Test
    void nonMemberIs404BeforeAnyLookup() {
        doThrow(new ResourceNotFoundException(TourOperatorMembershipCheck.TENANT_NOT_FOUND))
                .when(membershipCheck).ensureMember(callerId, operatorId);

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(operatorId, userId, callerId));
        verify(memberRepository, never()).findByTourOperatorIdAndUserId(any(), any());
    }

    @Test
    void unknownOrCrossTenantMemberIs404() {
        when(memberRepository.findByTourOperatorIdAndUserId(operatorId, userId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(operatorId, userId, callerId));
    }
}
