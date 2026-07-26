package com.vointika.touroperator.application.usecase;

import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.GoneException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.InvitedUserProvisioning;
import com.vointika.shared.port.InvitedUserProvisioning.ProvisionedUser;
import com.vointika.shared.port.InvitedUserProvisioning.SessionTokens;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.port.UserContactView;
import com.vointika.shared.service.IdGenerator;
import com.vointika.touroperator.application.port.InvitationTokenPort;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.entity.TourOperatorInvitation;
import com.vointika.touroperator.domain.entity.TourOperatorMember;
import com.vointika.touroperator.domain.enums.InvitationStatus;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.TourOperatorInvitationRepository;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.touroperator.domain.valueobject.InviteeEmail;
import com.vointika.touroperator.domain.valueobject.InviteeName;
import com.vointika.shared.valueobject.Slug;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AcceptInvitationUseCaseTest {

    private final AuditTrailPort auditTrailPort = mock(AuditTrailPort.class);

    private TourOperatorInvitationRepository invitationRepository;
    private TourOperatorMemberRepository memberRepository;
    private TourOperatorRepository tourOperatorRepository;
    private UserAccountQuery userAccountQuery;
    private InvitedUserProvisioning invitedUserProvisioning;
    private InvitationTokenPort tokenPort;
    private AcceptInvitationUseCase useCase;

    private final UUID operatorId = UUID.randomUUID();
    private static final String INVITEE = "invitee@example.com";

    @BeforeEach
    void setUp() {
        invitationRepository = mock(TourOperatorInvitationRepository.class);
        memberRepository = mock(TourOperatorMemberRepository.class);
        tourOperatorRepository = mock(TourOperatorRepository.class);
        userAccountQuery = mock(UserAccountQuery.class);
        invitedUserProvisioning = mock(InvitedUserProvisioning.class);
        tokenPort = mock(InvitationTokenPort.class);
        IdGenerator idGenerator = mock(IdGenerator.class);
        TransactionRunner transactionRunner = new TransactionRunner() {
            @Override public <T> T call(Supplier<T> work) { return work.get(); }
            @Override public void run(Runnable work) { work.run(); }
        };
        useCase = new AcceptInvitationUseCase(invitationRepository, memberRepository,
                tourOperatorRepository, userAccountQuery, invitedUserProvisioning,
                tokenPort, idGenerator, transactionRunner, auditTrailPort);

        when(idGenerator.newId()).thenReturn(UUID.randomUUID());
        when(tokenPort.hash("raw")).thenReturn("hash");
        when(tourOperatorRepository.findById(operatorId)).thenReturn(Optional.of(operator()));
        when(memberRepository.save(any())).thenAnswer(a -> a.getArgument(0));
        when(invitationRepository.save(any())).thenAnswer(a -> a.getArgument(0));
    }

    private void tokenResolvesTo(InvitationStatus status, Instant expiresAt) {
        TourOperatorInvitation invitation = new TourOperatorInvitation(
                UUID.randomUUID(), operatorId, new InviteeEmail(INVITEE), new InviteeName("Test Invitee"), MemberRole.STAFF,
                "hash", status, UUID.randomUUID(), "Olive Inviter",
                Instant.parse("2026-01-01T00:00:00Z"), expiresAt, null);
        when(invitationRepository.findByTokenHash("hash")).thenReturn(Optional.of(invitation));
    }

    private void validPendingToken() {
        tokenResolvesTo(InvitationStatus.PENDING, Instant.parse("2030-01-01T00:00:00Z"));
    }

    private TourOperator operator() {
        return new TourOperator(operatorId, new TourOperatorName("Acme Tours"),
                new Slug("acme-tours"), UUID.randomUUID(), UUID.randomUUID(),
                new TourOperatorAddress("123 Beach Rd"), UUID.randomUUID());
    }

    // ---- token matrix ----

    @Test
    void unknownTokenIs404() {
        when(invitationRepository.findByTokenHash("hash")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute("raw", null, "Ada", "Password1!"));
    }

    @Test
    void alreadyAcceptedIs409() {
        tokenResolvesTo(InvitationStatus.ACCEPTED, Instant.parse("2030-01-01T00:00:00Z"));
        assertThrows(ConflictException.class, () -> useCase.execute("raw", null, "Ada", "Password1!"));
    }

    @Test
    void revokedOrExpiredIs410() {
        tokenResolvesTo(InvitationStatus.REVOKED, Instant.parse("2030-01-01T00:00:00Z"));
        assertThrows(GoneException.class, () -> useCase.execute("raw", null, "Ada", "Password1!"));

        tokenResolvesTo(InvitationStatus.PENDING, Instant.parse("2026-01-02T00:00:00Z")); // past
        assertThrows(GoneException.class, () -> useCase.execute("raw", null, "Ada", "Password1!"));
    }

    // ---- authenticated branch ----

    @Test
    void authenticatedWithAMismatchedEmailIs403() {
        validPendingToken();
        UUID caller = UUID.randomUUID();
        when(userAccountQuery.findContact(caller))
                .thenReturn(Optional.of(new UserContactView("someone.else@example.com", "Someone Else", "en")));
        assertThrows(ForbiddenException.class, () -> useCase.execute("raw", caller, null, null));
    }

    @Test
    void authenticatedMatchingEmailJoinsWithNoNewSession() {
        validPendingToken();
        UUID caller = UUID.randomUUID();
        when(userAccountQuery.findContact(caller))
                .thenReturn(Optional.of(new UserContactView(INVITEE, "Invited User", "en")));
        when(memberRepository.existsByTourOperatorIdAndUserId(operatorId, caller)).thenReturn(false);
        when(memberRepository.existsByUserId(caller)).thenReturn(false);

        AcceptInvitationUseCase.Result result = useCase.execute("raw", caller, null, null);

        assertNull(result.tokens(), "an already-authenticated accepter keeps their session");
        ArgumentCaptor<TourOperatorMember> member = ArgumentCaptor.forClass(TourOperatorMember.class);
        verify(memberRepository).save(member.capture());
        assertEquals(MemberRole.STAFF, member.getValue().getRole());
        verify(invitationRepository).save(any()); // accepted
    }

    // ---- anonymous branch ----

    @Test
    void anonymousWithoutCredentialsIs422() {
        validPendingToken();
        assertThrows(InvalidFieldException.class, () -> useCase.execute("raw", null, null, null));
        assertThrows(InvalidFieldException.class, () -> useCase.execute("raw", null, "Ada", " "));
    }

    @Test
    void anonymousWhenEmailAlreadyHasAnAccountIs409() {
        validPendingToken();
        when(userAccountQuery.findUserIdByEmail(INVITEE)).thenReturn(Optional.of(UUID.randomUUID()));
        assertThrows(ConflictException.class, () -> useCase.execute("raw", null, "Ada", "Password1!"));
    }

    @Test
    void anonymousProvisionsVerifiedUserJoinsAndIssuesASession() {
        validPendingToken();
        UUID newUser = UUID.randomUUID();
        when(userAccountQuery.findUserIdByEmail(INVITEE)).thenReturn(Optional.empty());
        when(invitedUserProvisioning.findOrCreateVerifiedUser(INVITEE, "Ada", "Password1!"))
                .thenReturn(new ProvisionedUser(newUser, true));
        when(memberRepository.existsByTourOperatorIdAndUserId(operatorId, newUser)).thenReturn(false);
        when(memberRepository.existsByUserId(newUser)).thenReturn(false);
        when(invitedUserProvisioning.issueSession(newUser))
                .thenReturn(new SessionTokens("access-jwt", "refresh-tok"));

        AcceptInvitationUseCase.Result result = useCase.execute("raw", null, "Ada", "Password1!");

        assertEquals("access-jwt", result.tokens().accessToken());
        ArgumentCaptor<TourOperatorMember> member = ArgumentCaptor.forClass(TourOperatorMember.class);
        verify(memberRepository).save(member.capture());
        assertEquals(MemberRole.STAFF, member.getValue().getRole());
        assertEquals(newUser, member.getValue().getUserId());
    }

    @Test
    void anonymousWhenProvisioningFindsAnExistingAccountIs409() {
        validPendingToken();
        when(userAccountQuery.findUserIdByEmail(INVITEE)).thenReturn(Optional.empty());
        when(invitedUserProvisioning.findOrCreateVerifiedUser(INVITEE, "Ada", "Password1!"))
                .thenReturn(new ProvisionedUser(UUID.randomUUID(), false)); // not created — race
        assertThrows(ConflictException.class, () -> useCase.execute("raw", null, "Ada", "Password1!"));
    }
}
