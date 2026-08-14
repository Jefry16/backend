package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.touroperator.application.dto.output.InvitationView;
import com.vointika.touroperator.domain.entity.TourOperatorInvitation;
import com.vointika.touroperator.domain.enums.InvitationStatus;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.TourOperatorInvitationRepository;
import com.vointika.touroperator.domain.valueobject.InviteeEmail;
import com.vointika.touroperator.domain.valueobject.InviteeName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetInvitationUseCaseTest {

    private TourOperatorInvitationRepository invitationRepository;
    private TourOperatorMembershipCheck membershipCheck;
    private GetInvitationUseCase useCase;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID invitationId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();
    private final UUID inviterId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        invitationRepository = mock(TourOperatorInvitationRepository.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        useCase = new GetInvitationUseCase(invitationRepository, membershipCheck);
    }

    private TourOperatorInvitation invitation(InvitationStatus status, Instant expiresAt) {
        return new TourOperatorInvitation(
                invitationId, operatorId, new InviteeEmail("teammate@example.com"), new InviteeName("Test Invitee"),
                MemberRole.STAFF, "hash", status, inviterId, "Olive Inviter",
                Instant.parse("2026-01-01T00:00:00Z"), expiresAt, null);
    }

    @Test
    void returnsTheInvitationDetailForAnyMember() {
        when(invitationRepository.findByIdAndTourOperatorId(invitationId, operatorId))
                .thenReturn(Optional.of(invitation(InvitationStatus.PENDING,
                        Instant.parse("2999-01-01T00:00:00Z"))));

        InvitationView view = useCase.execute(operatorId, invitationId, callerId);

        // Any member may view (STAFF included) — the gate is ensureMember, not ensureAdmin.
        verify(membershipCheck).ensureMember(callerId, operatorId);
        assertEquals(invitationId, view.id());
        assertEquals("teammate@example.com", view.email());
        assertEquals(MemberRole.STAFF, view.role());
        assertEquals(InvitationStatus.PENDING, view.status());
        assertFalse(view.expired());
        assertEquals(inviterId, view.invitedByUserId());
        // Inviter name is the frozen snapshot carried on the invitation row.
        assertEquals("Olive Inviter", view.invitedByName());
    }

    @Test
    void flagsALapsedPendingInvitationAsExpired() {
        when(invitationRepository.findByIdAndTourOperatorId(invitationId, operatorId))
                .thenReturn(Optional.of(invitation(InvitationStatus.PENDING,
                        Instant.parse("2000-01-01T00:00:00Z"))));

        InvitationView view = useCase.execute(operatorId, invitationId, callerId);

        assertEquals(InvitationStatus.PENDING, view.status());
        assertTrue(view.expired());
    }

    @Test
    void anAcceptedInvitationIsNotReportedExpiredEvenPastItsWindow() {
        when(invitationRepository.findByIdAndTourOperatorId(invitationId, operatorId))
                .thenReturn(Optional.of(invitation(InvitationStatus.ACCEPTED,
                        Instant.parse("2000-01-01T00:00:00Z"))));

        InvitationView view = useCase.execute(operatorId, invitationId, callerId);

        assertEquals(InvitationStatus.ACCEPTED, view.status());
        assertFalse(view.expired());
    }

    @Test
    void nonMemberIs404BeforeAnyLookup() {
        doThrow(new ResourceNotFoundException("Tour operator not found"))
                .when(membershipCheck).ensureMember(callerId, operatorId);

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(operatorId, invitationId, callerId));
        verify(invitationRepository, never()).findByIdAndTourOperatorId(any(), any());
    }

    @Test
    void unknownOrCrossTenantInvitationIs404() {
        when(invitationRepository.findByIdAndTourOperatorId(invitationId, operatorId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(operatorId, invitationId, callerId));
    }
}
