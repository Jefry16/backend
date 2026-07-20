package com.vointika.touroperator.presentation.request;

/** Body of {@code POST .../invitations}: who to invite and as what role. */
public record InviteTeamMemberRequest(String email, String role) {}
