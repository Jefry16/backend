package com.vointika.touroperator.presentation.request;

/** Body of {@code PATCH .../members/{userId}}: the role to set (OWNER = transfer). */
public record ChangeMemberRoleRequest(String role) {}
