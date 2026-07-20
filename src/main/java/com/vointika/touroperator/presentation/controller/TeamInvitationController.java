package com.vointika.touroperator.presentation.controller;

import com.vointika.touroperator.application.usecase.InviteTeamMemberUseCase;
import com.vointika.touroperator.presentation.request.InviteTeamMemberRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * Team invitations (admin side). Membership on the operator is enforced by the
 * {@code /api/tour-operators/**} interceptor (non-member → 404); the use case
 * adds the ADMIN+ role gate. The invitee's accept flow lives under the flat
 * {@code /api/invitations} path (not operator-scoped).
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/invitations")
public class TeamInvitationController {

    private final InviteTeamMemberUseCase inviteTeamMemberUseCase;

    public TeamInvitationController(InviteTeamMemberUseCase inviteTeamMemberUseCase) {
        this.inviteTeamMemberUseCase = inviteTeamMemberUseCase;
    }

    @PostMapping
    public ResponseEntity<Void> invite(
            @PathVariable UUID tourOperatorId,
            @RequestBody InviteTeamMemberRequest body,
            @AuthenticationPrincipal String userIdStr) {
        UUID invitationId = inviteTeamMemberUseCase.execute(
                tourOperatorId, UUID.fromString(userIdStr), body.email(), body.role());
        return ResponseEntity
                .created(URI.create("/api/tour-operators/" + tourOperatorId
                        + "/invitations/" + invitationId))
                .build();
    }
}
