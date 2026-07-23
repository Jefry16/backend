package com.vointika.touroperator.application.usecase;

import com.vointika.shared.event.TeamInvitationRequestedEvent;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.EventPublisherPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.port.UserContactView;
import com.vointika.touroperator.application.port.InvitationTokenPort;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.entity.TourOperatorInvitation;
import com.vointika.touroperator.domain.repository.TourOperatorInvitationRepository;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;

import java.util.UUID;

/**
 * Re-sends a pending team invitation: mints a FRESH token (the previous accept
 * link dies), extends the validity window, and re-publishes the invite email.
 * Membership on the operator is enforced by the route interceptor; this adds the
 * role gate — only an ADMIN+ may resend (mirrors invite).
 *
 * <p>Guards: caller not ADMIN+ → 403; the invitation isn't under this operator
 * → 404; already accepted or revoked → 409 (only pending invitations can be
 * resent — a lapsed-but-pending one CAN, that's the point). The email is sent in
 * the resending admin's UI language (the invitee still has no account/language);
 * the event carries the RAW token, published after the save. No audit entry
 * (no audit context yet).
 */
public class ResendInvitationUseCase {

    private final TourOperatorInvitationRepository invitationRepository;
    private final TourOperatorRepository tourOperatorRepository;
    private final UserAccountQuery userAccountQuery;
    private final TourOperatorMembershipCheck membershipCheck;
    private final InvitationTokenPort invitationTokenPort;
    private final EventPublisherPort eventPublisher;

    public ResendInvitationUseCase(TourOperatorInvitationRepository invitationRepository,
                                   TourOperatorRepository tourOperatorRepository,
                                   UserAccountQuery userAccountQuery,
                                   TourOperatorMembershipCheck membershipCheck,
                                   InvitationTokenPort invitationTokenPort,
                                   EventPublisherPort eventPublisher) {
        this.invitationRepository = invitationRepository;
        this.tourOperatorRepository = tourOperatorRepository;
        this.userAccountQuery = userAccountQuery;
        this.membershipCheck = membershipCheck;
        this.invitationTokenPort = invitationTokenPort;
        this.eventPublisher = eventPublisher;
    }

    public void execute(UUID tourOperatorId, UUID invitationId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        TourOperatorInvitation invitation = invitationRepository
                .findByIdAndTourOperatorId(invitationId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
        TourOperator operator = tourOperatorRepository.findById(tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour operator not found"));

        String rawToken = invitationTokenPort.generate();
        invitation.renew(invitationTokenPort.hash(rawToken));
        invitationRepository.save(invitation);

        String locale = userAccountQuery.findContact(callerUserId)
                .map(UserContactView::language).orElse("en");
        eventPublisher.publish(new TeamInvitationRequestedEvent(
                invitation.getEmail().value(), invitation.getName().value(),
                operator.getName().value(), invitation.getRole().name(), rawToken, locale));
    }
}
