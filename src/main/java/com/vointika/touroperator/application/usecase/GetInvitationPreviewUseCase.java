package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.exception.GoneException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.touroperator.application.port.InvitationTokenPort;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.entity.TourOperatorInvitation;
import com.vointika.touroperator.domain.enums.InvitationStatus;
import com.vointika.touroperator.domain.repository.TourOperatorInvitationRepository;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;

import java.time.Instant;

/**
 * Validates an invitation token WITHOUT accepting — the accept page calls this on
 * load so an invalid/expired link fails before the invitee fills the form. Same
 * gate as accept (404/409/410); returns the operator name + the invitee email the
 * invitation was issued to (safe to expose to the link holder — the token arrived
 * in an email to that address).
 */
public class GetInvitationPreviewUseCase {

    private final TourOperatorInvitationRepository invitationRepository;
    private final TourOperatorRepository tourOperatorRepository;
    private final InvitationTokenPort invitationTokenPort;

    public GetInvitationPreviewUseCase(TourOperatorInvitationRepository invitationRepository,
                                       TourOperatorRepository tourOperatorRepository,
                                       InvitationTokenPort invitationTokenPort) {
        this.invitationRepository = invitationRepository;
        this.tourOperatorRepository = tourOperatorRepository;
        this.invitationTokenPort = invitationTokenPort;
    }

    public Preview execute(String rawToken) {
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

        return new Preview(operator.getName().value(), invitation.getEmail().value());
    }

    public record Preview(String operatorName, String email) {}
}
