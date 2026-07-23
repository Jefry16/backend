package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.touroperator.domain.entity.TourOperatorInvitation;
import com.vointika.touroperator.domain.enums.InvitationStatus;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.TourOperatorInvitationRepository;
import com.vointika.touroperator.domain.valueobject.InviteeEmail;
import com.vointika.touroperator.domain.valueobject.InviteeName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RevokeInvitationUseCaseTest {

    private TourOperatorInvitationRepository invitationRepository;
    private TourOperatorMembershipCheck membershipCheck;
    private RevokeInvitationUseCase useCase;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID invitationId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        invitationRepository = mock(TourOperatorInvitationRepository.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        useCase = new RevokeInvitationUseCase(invitationRepository, membershipCheck);
        when(invitationRepository.save(any())).thenAnswer(a -> a.getArgument(0));
    }

    private TourOperatorInvitation invitationWith(InvitationStatus status) {
        return new TourOperatorInvitation(
                invitationId, operatorId, new InviteeEmail("teammate@example.com"), new InviteeName("Test Invitee"),
                MemberRole.STAFF, "hash", status, UUID.randomUUID(), "Olive Inviter",
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2030-01-01T00:00:00Z"), null);
    }

    @Test
    void revokesAPendingInvitation() {
        when(invitationRepository.findByIdAndTourOperatorId(invitationId, operatorId))
                .thenReturn(Optional.of(invitationWith(InvitationStatus.PENDING)));

        useCase.execute(operatorId, invitationId, callerId);

        verify(membershipCheck).ensureAdmin(callerId, operatorId);
        ArgumentCaptor<TourOperatorInvitation> saved = ArgumentCaptor.forClass(TourOperatorInvitation.class);
        verify(invitationRepository).save(saved.capture());
        assertEquals(InvitationStatus.REVOKED, saved.getValue().getStatus());
    }

    @Test
    void nonAdminIsRejectedBeforeAnyLookup() {
        doThrow(new ForbiddenException("This action requires ADMIN privileges"))
                .when(membershipCheck).ensureAdmin(callerId, operatorId);

        assertThrows(ForbiddenException.class, () -> useCase.execute(operatorId, invitationId, callerId));

        verify(invitationRepository, never()).findByIdAndTourOperatorId(any(), any());
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void unknownOrCrossTenantInvitationIs404() {
        when(invitationRepository.findByIdAndTourOperatorId(eq(invitationId), eq(operatorId)))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(operatorId, invitationId, callerId));
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void revokingAnAlreadyAcceptedInvitationConflicts() {
        when(invitationRepository.findByIdAndTourOperatorId(invitationId, operatorId))
                .thenReturn(Optional.of(invitationWith(InvitationStatus.ACCEPTED)));

        assertThrows(ConflictException.class, () -> useCase.execute(operatorId, invitationId, callerId));
        verify(invitationRepository, never()).save(any());
    }
}
