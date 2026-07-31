package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.GoneException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.InvitedUserProvisioning;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.InvitedUserProvisioning.SessionTokens;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.port.UserContactView;
import com.vointika.shared.service.IdGenerator;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.touroperator.application.port.InvitationTokenPort;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.entity.TourOperatorInvitation;
import com.vointika.touroperator.domain.entity.TourOperatorMember;
import com.vointika.touroperator.domain.enums.InvitationStatus;
import com.vointika.touroperator.domain.repository.TourOperatorInvitationRepository;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.shared.exception.UniqueConstraintViolationException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Accepts a team invitation. The raw token is hashed and looked up
 * ({@code token_hash} is the capability), then the locked matrix applies:
 * unknown → 404; ACCEPTED → 409; REVOKED/expired-PENDING → 410.
 *
 * <ul>
 *   <li><b>authenticated caller:</b> the invitation email must match the caller's
 *       account email → else 403; no session is issued (they have one);</li>
 *   <li><b>anonymous caller:</b> name+password required (422); if the email already
 *       has an account → 409 ("log in to accept" — closes the pre-registered-email
 *       attack); otherwise provision a verified-from-birth user + issue a session.</li>
 * </ul>
 *
 * <p>Provisioning, membership, and the ACCEPTED transition run in ONE transaction;
 * the membership is {@code isDefault} only when it is the user's first. If the
 * caller already IS a member, the insert is skipped — accept stays idempotent.
 * The {@code invitation.accepted} audit entry (actor = the ACCEPTING user)
 * rides the accept transaction.
 */
public class AcceptInvitationUseCase {

    private final TourOperatorInvitationRepository invitationRepository;
    private final TourOperatorMemberRepository memberRepository;
    private final TourOperatorRepository tourOperatorRepository;
    private final UserAccountQuery userAccountQuery;
    private final InvitedUserProvisioning invitedUserProvisioning;
    private final InvitationTokenPort invitationTokenPort;
    private final IdGenerator idGenerator;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public AcceptInvitationUseCase(TourOperatorInvitationRepository invitationRepository,
                                   TourOperatorMemberRepository memberRepository,
                                   TourOperatorRepository tourOperatorRepository,
                                   UserAccountQuery userAccountQuery,
                                   InvitedUserProvisioning invitedUserProvisioning,
                                   InvitationTokenPort invitationTokenPort,
                                   IdGenerator idGenerator,
                                   TransactionRunner transactionRunner,
                                   AuditTrailPort auditTrailPort) {
        this.invitationRepository = invitationRepository;
        this.memberRepository = memberRepository;
        this.tourOperatorRepository = tourOperatorRepository;
        this.userAccountQuery = userAccountQuery;
        this.invitedUserProvisioning = invitedUserProvisioning;
        this.invitationTokenPort = invitationTokenPort;
        this.idGenerator = idGenerator;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    /** @param authenticatedUserId the caller's user id, or {@code null} for an anonymous accept. */
    public Result execute(String rawToken, UUID authenticatedUserId, String name, String password) {
        TourOperatorInvitation invitation = invitationRepository
                .findByTokenHash(invitationTokenPort.hash(rawToken))
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        if (invitation.getStatus() == InvitationStatus.ACCEPTED) {
            throw new ConflictException("This invitation has already been accepted");
        }
        if (invitation.getStatus() != InvitationStatus.PENDING || invitation.isExpired(Instant.now())) {
            throw new GoneException("This invitation is no longer valid");
        }

        TourOperator operator = tourOperatorRepository.findById(invitation.getTourOperatorId())
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
        String inviteeEmail = invitation.getEmail().value();

        if (authenticatedUserId != null) {
            UserContactView caller = userAccountQuery.findContact(authenticatedUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
            if (!inviteeEmail.equals(caller.email())) {
                throw new ForbiddenException("This invitation was issued to a different email address");
            }
            try {
                transactionRunner.run(() -> complete(
                        invitation, authenticatedUserId, caller.name(), caller.email()));
            } catch (UniqueConstraintViolationException e) {
                // Overlapping double-accept by the same invitee — the membership
                // already exists. Idempotent success, not a 500.
            }
            return new Result(operator.getId(), operator.getName().value(), null);
        }

        if (name == null || name.isBlank() || password == null || password.isBlank()) {
            throw new InvalidFieldException(
                    "name and password are required to accept without an account");
        }
        if (userAccountQuery.findUserIdByEmail(inviteeEmail).isPresent()) {
            throw new ConflictException(
                    "An account with this email already exists — log in to accept the invitation");
        }

        SessionTokens tokens;
        try {
            tokens = transactionRunner.call(() -> {
                InvitedUserProvisioning.ProvisionedUser provisioned =
                        invitedUserProvisioning.findOrCreateVerifiedUser(inviteeEmail, name, password);
                if (!provisioned.created()) {
                    // A competing registration committed before this lookup — refuse.
                    throw new ConflictException(
                            "An account with this email already exists — log in to accept the invitation");
                }
                complete(invitation, provisioned.userId(), name, inviteeEmail);
                return invitedUserProvisioning.issueSession(provisioned.userId());
            });
        } catch (UniqueConstraintViolationException e) {
            // Truly-concurrent registration tripped users_email_unique — fail closed with the same 409.
            throw new ConflictException(
                    "An account with this email already exists — log in to accept the invitation");
        }
        return new Result(operator.getId(), operator.getName().value(), tokens);
    }

    private void complete(TourOperatorInvitation invitation, UUID userId, String name, String email) {
        if (!memberRepository.existsByTourOperatorIdAndUserId(invitation.getTourOperatorId(), userId)) {
            boolean isFirstMembership = !memberRepository.existsByUserId(userId);
            memberRepository.save(new TourOperatorMember(
                    idGenerator.newId(), invitation.getTourOperatorId(), userId,
                    invitation.getRole(), isFirstMembership, name, email));
        }
        invitation.accept();
        invitationRepository.save(invitation);
        auditTrailPort.append(new NewAuditEntry(
                invitation.getTourOperatorId(), AuditActor.user(userId),
                "INVITATION", invitation.getId(), "invitation.accepted",
                Map.of("email", email)));
    }

    /** {@code tokens} is non-null only for a freshly provisioned user (auto-login). */
    public record Result(UUID tourOperatorId, String operatorName, SessionTokens tokens) {}
}
