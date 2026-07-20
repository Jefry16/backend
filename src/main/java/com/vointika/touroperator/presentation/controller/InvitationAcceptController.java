package com.vointika.touroperator.presentation.controller;

import com.vointika.shared.web.security.RefreshTokenCookieFactory;
import com.vointika.touroperator.application.usecase.AcceptInvitationUseCase;
import com.vointika.touroperator.application.usecase.GetInvitationPreviewUseCase;
import com.vointika.touroperator.presentation.request.AcceptInvitationRequest;
import com.vointika.touroperator.presentation.response.AcceptInvitationResponse;
import com.vointika.touroperator.presentation.response.InvitationPreviewResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * The invitee's side of team invitations, under the flat {@code /api/invitations}
 * base — NOT operator-scoped (the caller isn't a member yet, so the membership
 * interceptor must not apply). Both routes are PUBLIC: the emailed token is the
 * capability. The JWT filter still authenticates a Bearer token when one is
 * sent, so a logged-in user hits the same endpoint with a principal — the accept
 * use case forks on that.
 */
@RestController
@RequestMapping("/api/invitations")
public class InvitationAcceptController {

    private final AcceptInvitationUseCase acceptInvitationUseCase;
    private final GetInvitationPreviewUseCase getInvitationPreviewUseCase;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    public InvitationAcceptController(AcceptInvitationUseCase acceptInvitationUseCase,
                                      GetInvitationPreviewUseCase getInvitationPreviewUseCase,
                                      RefreshTokenCookieFactory refreshTokenCookieFactory) {
        this.acceptInvitationUseCase = acceptInvitationUseCase;
        this.getInvitationPreviewUseCase = getInvitationPreviewUseCase;
        this.refreshTokenCookieFactory = refreshTokenCookieFactory;
    }

    @GetMapping("/{token}/preview")
    public ResponseEntity<InvitationPreviewResponse> preview(@PathVariable String token) {
        return ResponseEntity.ok(
                InvitationPreviewResponse.from(getInvitationPreviewUseCase.execute(token)));
    }

    @PostMapping("/{token}/accept")
    public ResponseEntity<AcceptInvitationResponse> accept(
            @PathVariable String token,
            @RequestBody(required = false) AcceptInvitationRequest body,
            @AuthenticationPrincipal String userIdStr) {
        UUID authenticatedUserId = parseAuthenticatedUser(userIdStr);
        AcceptInvitationUseCase.Result result = acceptInvitationUseCase.execute(
                token,
                authenticatedUserId,
                body == null ? null : body.name(),
                body == null ? null : body.password());

        if (result.tokens() == null) {
            return ResponseEntity.ok(new AcceptInvitationResponse(
                    result.tourOperatorId(), result.operatorName(), null));
        }
        // New user: auto-login, mirroring the login endpoint — access token in the
        // body, refresh token as the standard httpOnly cookie.
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        refreshTokenCookieFactory.issue(result.tokens().refreshToken()).toString())
                .body(new AcceptInvitationResponse(
                        result.tourOperatorId(), result.operatorName(),
                        result.tokens().accessToken()));
    }

    /**
     * On the PUBLIC accept route an unauthenticated request reaches the controller
     * with Spring's anonymous principal ({@code "anonymousUser"}, a plain string)
     * rather than null. The JWT filter only ever sets UUID-string principals, so
     * anything non-UUID means "no session".
     */
    private static UUID parseAuthenticatedUser(String principal) {
        if (principal == null) {
            return null;
        }
        try {
            return UUID.fromString(principal);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
