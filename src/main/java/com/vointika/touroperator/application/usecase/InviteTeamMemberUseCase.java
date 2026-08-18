package com.vointika.touroperator.application.usecase;

import com.vointika.shared.event.TeamInvitationRequestedEvent;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.EventPublisherPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.port.UserContactView;
import com.vointika.shared.service.IdGenerator;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.touroperator.application.port.InvitationTokenPort;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.entity.TourOperatorInvitation;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.TourOperatorInvitationRepository;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.shared.valueobject.Email;
import com.vointika.touroperator.domain.valueobject.InviteeName;
import com.vointika.shared.exception.UniqueConstraintViolationException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Creates a PENDING team invitation and requests the invite email. Membership on
 * the operator is enforced by the route interceptor; this adds the role gate —
 * only an ADMIN+ may invite.
 *
 * <p>Guards: caller not ADMIN+ → 403; malformed email / role = OWNER or unknown
 * → 422; the email already belongs to a member of THIS operator → 409; a PENDING
 * invitation already exists → 409 (double-guarded by the partial unique index).
 * The event carries the RAW token (only its hash is persisted) and is published
 * after the save; the {@code member.invited} audit entry rides the save's
 * transaction.
 */
public class InviteTeamMemberUseCase {

    /** Thrown twice — the pre-check and the race answer identically. */
    private static final String PENDING_EXISTS =
            "A pending invitation for this email already exists";

    private final TourOperatorInvitationRepository invitationRepository;
    private final TourOperatorMemberRepository memberRepository;
    private final TourOperatorRepository tourOperatorRepository;
    private final UserAccountQuery userAccountQuery;
    private final TourOperatorMembershipCheck membershipCheck;
    private final InvitationTokenPort invitationTokenPort;
    private final IdGenerator idGenerator;
    private final EventPublisherPort eventPublisher;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public InviteTeamMemberUseCase(TourOperatorInvitationRepository invitationRepository,
                                   TourOperatorMemberRepository memberRepository,
                                   TourOperatorRepository tourOperatorRepository,
                                   UserAccountQuery userAccountQuery,
                                   TourOperatorMembershipCheck membershipCheck,
                                   InvitationTokenPort invitationTokenPort,
                                   IdGenerator idGenerator,
                                   EventPublisherPort eventPublisher,
                                   TransactionRunner transactionRunner,
                                   AuditTrailPort auditTrailPort) {
        this.invitationRepository = invitationRepository;
        this.memberRepository = memberRepository;
        this.tourOperatorRepository = tourOperatorRepository;
        this.userAccountQuery = userAccountQuery;
        this.membershipCheck = membershipCheck;
        this.invitationTokenPort = invitationTokenPort;
        this.idGenerator = idGenerator;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public UUID execute(UUID tourOperatorId, UUID invitedByUserId,
                        String rawEmail, String rawName, String rawRole) {
        membershipCheck.ensureAdmin(invitedByUserId, tourOperatorId);
        Email email = new Email(rawEmail);
        InviteeName name = new InviteeName(rawName);
        MemberRole role = parseRole(rawRole);
        TourOperator operator = tourOperatorRepository.requireById(tourOperatorId);

        Optional<UUID> existingUserId = userAccountQuery.findUserIdByEmail(email.value());
        if (existingUserId.isPresent()
                && memberRepository.existsByTourOperatorIdAndUserId(tourOperatorId, existingUserId.get())) {
            throw new ResourceAlreadyExistsException("This email already belongs to a team member");
        }
        if (invitationRepository.existsPendingByTourOperatorIdAndEmail(tourOperatorId, email.value())) {
            throw new ResourceAlreadyExistsException(PENDING_EXISTS);
        }

        // The inviter's contact: their name is snapshotted on the invitation, and
        // the invite email goes in their UI language (the invitee has no account
        // yet). They're the authenticated admin, so the account always resolves.
        UserContactView inviter = userAccountQuery.findContact(invitedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Inviting user not found"));

        String rawToken = invitationTokenPort.generate();
        TourOperatorInvitation invitation = TourOperatorInvitation.issue(
                idGenerator.newId(), tourOperatorId, email, name, role,
                invitationTokenPort.hash(rawToken), invitedByUserId, inviter.name());
        try {
            transactionRunner.run(() -> {
                invitationRepository.save(invitation);
                auditTrailPort.append(new NewAuditEntry(
                        tourOperatorId, AuditActor.user(invitedByUserId),
                        "INVITATION", invitation.getId(), "member.invited",
                        Map.of("email", email.value(), "role", role.name())));
            });
        } catch (UniqueConstraintViolationException e) {
            // A concurrent invite committed first — the partial unique index fired.
            throw new ResourceAlreadyExistsException(PENDING_EXISTS);
        }

        eventPublisher.publish(new TeamInvitationRequestedEvent(
                email.value(), name.value(), operator.getName().value(),
                role.name(), rawToken, inviter.language()));
        return invitation.getId();
    }

    /** An invite role must be ADMIN or STAFF. OWNER is transfer-only (never invited) → 422. */
    private MemberRole parseRole(String rawRole) {
        if ("ADMIN".equals(rawRole)) {
            return MemberRole.ADMIN;
        }
        if ("STAFF".equals(rawRole)) {
            return MemberRole.STAFF;
        }
        throw new InvalidFieldException("Role must be one of: ADMIN, STAFF");
    }
}
