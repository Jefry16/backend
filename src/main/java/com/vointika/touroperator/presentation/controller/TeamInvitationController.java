package com.vointika.touroperator.presentation.controller;

import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.web.list.CursorPageResponse;
import com.vointika.shared.web.list.ListQueryParser;
import com.vointika.touroperator.application.dto.output.InvitationView;
import com.vointika.touroperator.application.usecase.GetInvitationUseCase;
import com.vointika.touroperator.application.usecase.InviteTeamMemberUseCase;
import com.vointika.touroperator.application.usecase.ListInvitationsUseCase;
import com.vointika.touroperator.application.usecase.ResendInvitationUseCase;
import com.vointika.touroperator.application.usecase.RevokeInvitationUseCase;
import com.vointika.touroperator.presentation.request.InviteTeamMemberRequest;
import com.vointika.touroperator.presentation.response.InvitationResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final ListInvitationsUseCase listInvitationsUseCase;
    private final GetInvitationUseCase getInvitationUseCase;
    private final ResendInvitationUseCase resendInvitationUseCase;
    private final RevokeInvitationUseCase revokeInvitationUseCase;
    private final ListQueryParser listQueryParser;

    public TeamInvitationController(InviteTeamMemberUseCase inviteTeamMemberUseCase,
                                    ListInvitationsUseCase listInvitationsUseCase,
                                    GetInvitationUseCase getInvitationUseCase,
                                    ResendInvitationUseCase resendInvitationUseCase,
                                    RevokeInvitationUseCase revokeInvitationUseCase,
                                    ListQueryParser listQueryParser) {
        this.inviteTeamMemberUseCase = inviteTeamMemberUseCase;
        this.listInvitationsUseCase = listInvitationsUseCase;
        this.getInvitationUseCase = getInvitationUseCase;
        this.resendInvitationUseCase = resendInvitationUseCase;
        this.revokeInvitationUseCase = revokeInvitationUseCase;
        this.listQueryParser = listQueryParser;
    }

    /**
     * All invitations for the operator — any status, cursor-paginated,
     * tenant-scoped. Any member may view; a non-member is a 404. Filter by
     * {@code status} and/or {@code role}, sort by {@code createdAt} (default,
     * newest first) or {@code id}; page with {@code cursor}.
     */
    @GetMapping
    public ResponseEntity<CursorPageResponse<InvitationResponse>> list(
            @PathVariable UUID tourOperatorId,
            @AuthenticationPrincipal UUID callerUserId,
            HttpServletRequest request) {
        ListQuery query = listQueryParser.parse(request, ListInvitationsUseCase.SCHEMA, tourOperatorId);
        CursorPage<InvitationView> page = listInvitationsUseCase.execute(query, callerUserId);
        return ResponseEntity.ok(CursorPageResponse.of(page, InvitationResponse::from));
    }

    @PostMapping
    public ResponseEntity<Void> invite(
            @PathVariable UUID tourOperatorId,
            @RequestBody InviteTeamMemberRequest body,
            @AuthenticationPrincipal UUID userId) {
        UUID invitationId = inviteTeamMemberUseCase.execute(
                tourOperatorId, userId, body.email(), body.name(), body.role());
        return ResponseEntity
                .created(URI.create("/api/tour-operators/" + tourOperatorId
                        + "/invitations/" + invitationId))
                .build();
    }

    /** A single invitation's detail (status + expiry). Any member may view; 404 if not under this operator. */
    @GetMapping("/{invitationId}")
    public ResponseEntity<InvitationResponse> get(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID invitationId,
            @AuthenticationPrincipal UUID userId) {
        InvitationView view = getInvitationUseCase.execute(
                tourOperatorId, invitationId, userId);
        return ResponseEntity.ok(InvitationResponse.from(view));
    }

    /** Re-issues the accept link (fresh token + renewed expiry) and re-sends the email. */
    @PostMapping("/{invitationId}/resend")
    public ResponseEntity<Void> resend(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID invitationId,
            @AuthenticationPrincipal UUID userId) {
        resendInvitationUseCase.execute(tourOperatorId, invitationId, userId);
        return ResponseEntity.noContent().build();
    }

    /** Cancels a pending invitation (the accept link stops working). */
    @DeleteMapping("/{invitationId}")
    public ResponseEntity<Void> revoke(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID invitationId,
            @AuthenticationPrincipal UUID userId) {
        revokeInvitationUseCase.execute(tourOperatorId, invitationId, userId);
        return ResponseEntity.noContent().build();
    }
}
