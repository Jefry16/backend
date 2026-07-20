package com.vointika.touroperator.presentation.request;

/**
 * Body of {@code POST /api/invitations/{token}/accept}. Both fields are for the
 * ANONYMOUS (new-user) branch and required there; an authenticated caller sends
 * an empty body (or none) — their account is the identity.
 */
public record AcceptInvitationRequest(String name, String password) {}
