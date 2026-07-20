package com.vointika.touroperator.presentation.response;

import java.util.UUID;

/**
 * Result of accepting an invitation. {@code accessToken} is present only when a
 * NEW user was provisioned (auto-login; the refresh token travels as the standard
 * httpOnly cookie, like login) — an already-authenticated accepter keeps their
 * session and gets {@code null}.
 */
public record AcceptInvitationResponse(
        UUID tourOperatorId,
        String operatorName,
        String accessToken
) {}
