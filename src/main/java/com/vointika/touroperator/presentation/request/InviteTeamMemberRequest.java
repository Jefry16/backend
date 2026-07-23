package com.vointika.touroperator.presentation.request;

/** Body of {@code POST .../invitations}: who to invite, their name, and role. */
public record InviteTeamMemberRequest(String email, String name, String role) {}
