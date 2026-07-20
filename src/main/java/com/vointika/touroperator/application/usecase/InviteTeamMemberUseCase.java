package com.vointika.touroperator.application.usecase;

import com.vointika.shared.event.TeamInvitationRequestedEvent;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.EventPublisherPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.port.UserContactView;
import com.vointika.shared.service.IdGenerator;
import com.vointika.touroperator.application.port.InvitationTokenPort;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.entity.TourOperatorInvitation;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.TourOperatorInvitationRepository;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.touroperator.domain.valueobject.InviteeEmail;
import org.springframework.dao.DataIntegrityViolationException;

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
 * after the save. Create-slice scope: no audit entry (no audit context yet).
 */
public class InviteTeamMemberUseCase {

    private final TourOperatorInvitationRepository invitationRepository;
    private final TourOperatorMemberRepository memberRepository;
    private final TourOperatorRepository tourOperatorRepository;
    private final UserAccountQuery userAccountQuery;
    private final TourOperatorMembershipCheck membershipCheck;
    private final InvitationTokenPort invitationTokenPort;
    private final IdGenerator idGenerator;
    private final EventPublisherPort eventPublisher;

    public InviteTeamMemberUseCase(TourOperatorInvitationRepository invitationRepository,
                                   TourOperatorMemberRepository memberRepository,
                                   TourOperatorRepository tourOperatorRepository,
                                   UserAccountQuery userAccountQuery,
                                   TourOperatorMembershipCheck membershipCheck,
                                   InvitationTokenPort invitationTokenPort,
                                   IdGenerator idGenerator,
                                   EventPublisherPort eventPublisher) {
        this.invitationRepository = invitationRepository;
        this.memberRepository = memberRepository;
        this.tourOperatorRepository = tourOperatorRepository;
        this.userAccountQuery = userAccountQuery;
        this.membershipCheck = membershipCheck;
        this.invitationTokenPort = invitationTokenPort;
        this.idGenerator = idGenerator;
        this.eventPublisher = eventPublisher;
    }

    public UUID execute(UUID tourOperatorId, UUID invitedByUserId, String rawEmail, String rawRole) {
        membershipCheck.ensureAdmin(invitedByUserId, tourOperatorId);
        InviteeEmail email = new InviteeEmail(rawEmail);
        MemberRole role = parseRole(rawRole);
        TourOperator operator = tourOperatorRepository.findById(tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour operator not found"));

        Optional<UUID> existingUserId = userAccountQuery.findUserIdByEmail(email.value());
        if (existingUserId.isPresent()
                && memberRepository.existsByTourOperatorIdAndUserId(tourOperatorId, existingUserId.get())) {
            throw new ResourceAlreadyExistsException("This email already belongs to a team member");
        }
        if (invitationRepository.existsPendingByTourOperatorIdAndEmail(tourOperatorId, email.value())) {
            throw new ResourceAlreadyExistsException("A pending invitation for this email already exists");
        }

        String rawToken = invitationTokenPort.generate();
        TourOperatorInvitation invitation = TourOperatorInvitation.issue(
                idGenerator.newId(), tourOperatorId, email, role,
                invitationTokenPort.hash(rawToken), invitedByUserId);
        try {
            invitationRepository.save(invitation);
        } catch (DataIntegrityViolationException e) {
            // A concurrent invite committed first — the partial unique index fired.
            throw new ResourceAlreadyExistsException("A pending invitation for this email already exists");
        }

        // The invite email is sent in the inviting user's UI language (the invitee
        // has no account/language yet).
        String locale = userAccountQuery.findContact(invitedByUserId)
                .map(UserContactView::language).orElse("en");
        eventPublisher.publish(new TeamInvitationRequestedEvent(
                email.value(), operator.getName().value(), role.name(), rawToken, locale));
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
