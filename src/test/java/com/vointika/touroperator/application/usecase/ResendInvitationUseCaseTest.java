package com.vointika.touroperator.application.usecase;

import com.vointika.shared.event.TeamInvitationRequestedEvent;
import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.EventPublisherPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.port.UserContactView;
import com.vointika.touroperator.application.port.InvitationTokenPort;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.entity.TourOperatorInvitation;
import com.vointika.touroperator.domain.enums.InvitationStatus;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.TourOperatorInvitationRepository;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.touroperator.domain.valueobject.InviteeEmail;
import com.vointika.touroperator.domain.valueobject.Slug;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

class ResendInvitationUseCaseTest {

    private TourOperatorInvitationRepository invitationRepository;
    private TourOperatorRepository tourOperatorRepository;
    private UserAccountQuery userAccountQuery;
    private TourOperatorMembershipCheck membershipCheck;
    private InvitationTokenPort tokenPort;
    private EventPublisherPort eventPublisher;
    private ResendInvitationUseCase useCase;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID invitationId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        invitationRepository = mock(TourOperatorInvitationRepository.class);
        tourOperatorRepository = mock(TourOperatorRepository.class);
        userAccountQuery = mock(UserAccountQuery.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        tokenPort = mock(InvitationTokenPort.class);
        eventPublisher = mock(EventPublisherPort.class);
        useCase = new ResendInvitationUseCase(invitationRepository, tourOperatorRepository,
                userAccountQuery, membershipCheck, tokenPort, eventPublisher);

        when(tokenPort.generate()).thenReturn("fresh-raw");
        when(tokenPort.hash("fresh-raw")).thenReturn("fresh-hash");
        when(invitationRepository.save(any())).thenAnswer(a -> a.getArgument(0));
        when(tourOperatorRepository.findById(operatorId)).thenReturn(Optional.of(operator()));
        when(userAccountQuery.findContact(callerId))
                .thenReturn(Optional.of(new UserContactView("admin@example.com", "Admin", "es")));
    }

    private TourOperator operator() {
        return new TourOperator(operatorId, new TourOperatorName("Acme Tours"),
                new Slug("acme-tours"), UUID.randomUUID(), UUID.randomUUID(),
                new TourOperatorAddress("123 Beach Rd"), UUID.randomUUID());
    }

    private TourOperatorInvitation invitationWith(InvitationStatus status) {
        return new TourOperatorInvitation(
                invitationId, operatorId, new InviteeEmail("teammate@example.com"),
                MemberRole.ADMIN, "old-hash", status, UUID.randomUUID(),
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2020-01-01T00:00:00Z"), null);
    }

    @Test
    void mintsFreshTokenSavesAndRepublishesEmailInCallerLocale() {
        when(invitationRepository.findByIdAndTourOperatorId(invitationId, operatorId))
                .thenReturn(Optional.of(invitationWith(InvitationStatus.PENDING)));

        useCase.execute(operatorId, invitationId, callerId);

        verify(membershipCheck).ensureAdmin(callerId, operatorId);

        ArgumentCaptor<TourOperatorInvitation> saved = ArgumentCaptor.forClass(TourOperatorInvitation.class);
        verify(invitationRepository).save(saved.capture());
        assertEquals("fresh-hash", saved.getValue().getTokenHash());
        assertEquals(InvitationStatus.PENDING, saved.getValue().getStatus());

        ArgumentCaptor<TeamInvitationRequestedEvent> event =
                ArgumentCaptor.forClass(TeamInvitationRequestedEvent.class);
        verify(eventPublisher).publish(event.capture());
        assertEquals("teammate@example.com", event.getValue().email());
        assertEquals("Acme Tours", event.getValue().operatorName());
        assertEquals("ADMIN", event.getValue().role());
        assertEquals("fresh-raw", event.getValue().token());
        assertEquals("es", event.getValue().locale());
    }

    @Test
    void resendingALapsedButPendingInvitationSucceeds() {
        // expiresAt is in the past but status is still PENDING — resend renews it.
        when(invitationRepository.findByIdAndTourOperatorId(invitationId, operatorId))
                .thenReturn(Optional.of(invitationWith(InvitationStatus.PENDING)));

        useCase.execute(operatorId, invitationId, callerId);

        verify(invitationRepository).save(any());
        verify(eventPublisher).publish(any());
    }

    @Test
    void nonAdminIsRejectedBeforeAnyWork() {
        doThrow(new ForbiddenException("This action requires ADMIN privileges"))
                .when(membershipCheck).ensureAdmin(callerId, operatorId);

        assertThrows(ForbiddenException.class, () -> useCase.execute(operatorId, invitationId, callerId));

        verify(invitationRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void unknownOrCrossTenantInvitationIs404() {
        when(invitationRepository.findByIdAndTourOperatorId(invitationId, operatorId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(operatorId, invitationId, callerId));
        verify(invitationRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void resendingARevokedInvitationConflicts() {
        when(invitationRepository.findByIdAndTourOperatorId(invitationId, operatorId))
                .thenReturn(Optional.of(invitationWith(InvitationStatus.REVOKED)));

        assertThrows(ConflictException.class, () -> useCase.execute(operatorId, invitationId, callerId));
        verify(invitationRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void fallsBackToEnglishWhenCallerHasNoContact() {
        when(invitationRepository.findByIdAndTourOperatorId(invitationId, operatorId))
                .thenReturn(Optional.of(invitationWith(InvitationStatus.PENDING)));
        when(userAccountQuery.findContact(callerId)).thenReturn(Optional.empty());

        useCase.execute(operatorId, invitationId, callerId);

        ArgumentCaptor<TeamInvitationRequestedEvent> event =
                ArgumentCaptor.forClass(TeamInvitationRequestedEvent.class);
        verify(eventPublisher).publish(event.capture());
        assertEquals("en", event.getValue().locale());
    }
}
