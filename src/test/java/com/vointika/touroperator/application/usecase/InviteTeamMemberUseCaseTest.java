package com.vointika.touroperator.application.usecase;

import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.event.TeamInvitationRequestedEvent;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.port.EventPublisherPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.port.UserContactView;
import com.vointika.shared.service.IdGenerator;
import com.vointika.touroperator.application.port.InvitationTokenPort;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.repository.TourOperatorInvitationRepository;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.shared.valueobject.Handle;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InviteTeamMemberUseCaseTest {

    // Executes the work inline so assertions on the wrapped calls still hold.
    private final TransactionRunner transactionRunner = executingRunner();

    private static TransactionRunner executingRunner() {
        TransactionRunner runner = mock(TransactionRunner.class);
        when(runner.call(any())).thenAnswer(i -> ((java.util.function.Supplier<?>) i.getArgument(0)).get());
        doAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return null;
        }).when(runner).run(any());
        return runner;
    }

    private final AuditTrailPort auditTrailPort = mock(AuditTrailPort.class);

    private TourOperatorInvitationRepository invitationRepository;
    private TourOperatorMemberRepository memberRepository;
    private TourOperatorRepository tourOperatorRepository;
    private UserAccountQuery userAccountQuery;
    private TourOperatorMembershipCheck membershipCheck;
    private InvitationTokenPort tokenPort;
    private EventPublisherPort eventPublisher;
    private InviteTeamMemberUseCase useCase;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID inviterId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        invitationRepository = mock(TourOperatorInvitationRepository.class);
        memberRepository = mock(TourOperatorMemberRepository.class);
        tourOperatorRepository = mock(TourOperatorRepository.class);
        userAccountQuery = mock(UserAccountQuery.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        tokenPort = mock(InvitationTokenPort.class);
        eventPublisher = mock(EventPublisherPort.class);
        IdGenerator idGenerator = mock(IdGenerator.class);
        useCase = new InviteTeamMemberUseCase(invitationRepository, memberRepository,
                tourOperatorRepository, userAccountQuery, membershipCheck, tokenPort,
                idGenerator, eventPublisher, transactionRunner, auditTrailPort);

        when(idGenerator.newId()).thenReturn(UUID.randomUUID());
        when(tokenPort.generate()).thenReturn("raw");
        when(tokenPort.hash("raw")).thenReturn("hash");
        when(tourOperatorRepository.findById(operatorId)).thenReturn(Optional.of(operator()));
        when(invitationRepository.save(any())).thenAnswer(a -> a.getArgument(0));
        when(userAccountQuery.findContact(inviterId))
                .thenReturn(Optional.of(new UserContactView("inviter@example.com", "Inviter", "es")));
        // Run the real default: Mockito stubs a `default` like any other method,
        // so stubbing require* directly would make the assertions below hold for
        // any value (PATTERNS §9).
        doCallRealMethod().when(tourOperatorRepository).requireById(any());
    }

    private TourOperator operator() {
        return new TourOperator(operatorId, new TourOperatorName("Acme Tours"),
                new Handle("acme-tours"), UUID.randomUUID(), UUID.randomUUID(),
                new TourOperatorAddress("123 Beach Rd", null, "Palma", null, null, UUID.randomUUID()), UUID.randomUUID());
    }

    @Test
    void publishesInvitationEventWithRawTokenAndInviterLocale() {
        when(userAccountQuery.findUserIdByEmail("invitee@example.com")).thenReturn(Optional.empty());
        when(invitationRepository.existsPendingByTourOperatorIdAndEmail(operatorId, "invitee@example.com"))
                .thenReturn(false);

        useCase.execute(operatorId, inviterId, "Invitee@Example.com", "Test Invitee", "STAFF");

        verify(invitationRepository).save(any());
        ArgumentCaptor<TeamInvitationRequestedEvent> captor =
                ArgumentCaptor.forClass(TeamInvitationRequestedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        TeamInvitationRequestedEvent event = captor.getValue();
        assertEquals("invitee@example.com", event.email(), "email is normalized");
        assertEquals("Acme Tours", event.operatorName());
        assertEquals("STAFF", event.role());
        assertEquals("raw", event.token(), "the RAW token rides the event");
        assertEquals("es", event.locale(), "sent in the inviter's UI language");
    }

    @Test
    void staffCallerIsForbidden() {
        doThrow(new ForbiddenException("nope")).when(membershipCheck).ensureAdmin(inviterId, operatorId);
        assertThrows(ForbiddenException.class,
                () -> useCase.execute(operatorId, inviterId, "invitee@example.com", "Test Invitee", "STAFF"));
        verify(invitationRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void invitingAnOwnerIsRejected() {
        assertThrows(InvalidFieldException.class,
                () -> useCase.execute(operatorId, inviterId, "invitee@example.com", "Test Invitee", "OWNER"));
    }

    @Test
    void invitingWithABlankNameIsRejected() {
        assertThrows(InvalidFieldException.class,
                () -> useCase.execute(operatorId, inviterId, "invitee@example.com", "  ", "STAFF"));
    }

    @Test
    void invitingAnExistingTeamMemberConflicts() {
        UUID existing = UUID.randomUUID();
        when(userAccountQuery.findUserIdByEmail("invitee@example.com")).thenReturn(Optional.of(existing));
        when(memberRepository.existsByTourOperatorIdAndUserId(operatorId, existing)).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class,
                () -> useCase.execute(operatorId, inviterId, "invitee@example.com", "Test Invitee", "STAFF"));
    }

    @Test
    void duplicatePendingInvitationConflicts() {
        when(userAccountQuery.findUserIdByEmail("invitee@example.com")).thenReturn(Optional.empty());
        when(invitationRepository.existsPendingByTourOperatorIdAndEmail(operatorId, "invitee@example.com"))
                .thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class,
                () -> useCase.execute(operatorId, inviterId, "invitee@example.com", "Test Invitee", "STAFF"));
    }
}
