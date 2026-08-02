package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.exception.GoneException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.touroperator.application.port.InvitationTokenPort;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.entity.TourOperatorInvitation;
import com.vointika.touroperator.domain.enums.InvitationStatus;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.TourOperatorInvitationRepository;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.touroperator.domain.valueobject.InviteeEmail;
import com.vointika.touroperator.domain.valueobject.InviteeName;
import com.vointika.shared.valueobject.Handle;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetInvitationPreviewUseCaseTest {

    private TourOperatorInvitationRepository invitationRepository;
    private TourOperatorRepository tourOperatorRepository;
    private InvitationTokenPort tokenPort;
    private GetInvitationPreviewUseCase useCase;

    private final UUID operatorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        invitationRepository = mock(TourOperatorInvitationRepository.class);
        tourOperatorRepository = mock(TourOperatorRepository.class);
        tokenPort = mock(InvitationTokenPort.class);
        useCase = new GetInvitationPreviewUseCase(invitationRepository, tourOperatorRepository, tokenPort);
        when(tokenPort.hash("raw")).thenReturn("hash");
    }

    private TourOperatorInvitation invitation(InvitationStatus status, Instant expiresAt) {
        return new TourOperatorInvitation(
                UUID.randomUUID(), operatorId, new InviteeEmail("invitee@example.com"), new InviteeName("Test Invitee"),
                MemberRole.STAFF, "hash", status, UUID.randomUUID(), "Olive Inviter",
                Instant.parse("2026-01-01T00:00:00Z"), expiresAt, null);
    }

    private TourOperator operator() {
        return new TourOperator(operatorId, new TourOperatorName("Acme Tours"),
                new Handle("acme-tours"), UUID.randomUUID(), UUID.randomUUID(),
                new TourOperatorAddress("123 Beach Rd"), UUID.randomUUID());
    }

    @Test
    void returnsOperatorNameAndInviteeEmailForAValidToken() {
        when(invitationRepository.findByTokenHash("hash"))
                .thenReturn(Optional.of(invitation(InvitationStatus.PENDING, Instant.parse("2030-01-01T00:00:00Z"))));
        when(tourOperatorRepository.findById(operatorId)).thenReturn(Optional.of(operator()));

        GetInvitationPreviewUseCase.Preview preview = useCase.execute("raw");

        assertEquals("Acme Tours", preview.operatorName());
        assertEquals("invitee@example.com", preview.email());
    }

    @Test
    void unknownTokenIs404() {
        when(invitationRepository.findByTokenHash("hash")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute("raw"));
    }

    @Test
    void acceptedIs409AndExpiredIs410() {
        when(invitationRepository.findByTokenHash("hash"))
                .thenReturn(Optional.of(invitation(InvitationStatus.ACCEPTED, Instant.parse("2030-01-01T00:00:00Z"))));
        assertThrows(ConflictException.class, () -> useCase.execute("raw"));

        when(invitationRepository.findByTokenHash("hash"))
                .thenReturn(Optional.of(invitation(InvitationStatus.PENDING, Instant.parse("2026-01-02T00:00:00Z"))));
        assertThrows(GoneException.class, () -> useCase.execute("raw"));
    }
}
