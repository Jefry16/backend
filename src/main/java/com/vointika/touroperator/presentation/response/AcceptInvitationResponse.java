package com.vointika.touroperator.presentation.response;

import java.util.UUID;

/**
 * Result of accepting an invitation — the operator you joined ({@code id} +
 * {@code context:"tour-operators"}). {@code accessToken} is present only when a
 * NEW user was provisioned (auto-login; the refresh token travels as the standard
 * httpOnly cookie, like login) — an already-authenticated accepter keeps their
 * session and gets {@code null}.
 */
public record AcceptInvitationResponse(
        UUID id,
        String context,
        String operatorName,
        String accessToken
) {
    public AcceptInvitationResponse(UUID id, String operatorName, String accessToken) {
        this(id, "tour-operators", operatorName, accessToken);
    }
}
