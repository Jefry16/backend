package com.vointika.touroperator.presentation.response;

import com.vointika.touroperator.application.dto.output.MemberListView;
import com.vointika.touroperator.domain.enums.MemberRole;

import java.time.Instant;
import java.util.UUID;

/**
 * One team-roster row. A member is a user carrying a role, so {@code id} is the
 * user's id and {@code context} is {@code "users"} (house convention: the id
 * field is always {@code id}, and {@code context} names the entity's collection).
 * {@code name}/{@code email} are best-effort from identity (null if the account
 * can't be resolved).
 */
public record MemberResponse(
        UUID id,
        String context,
        MemberRole role,
        Instant joinedAt,
        String name,
        String email) {

    public MemberResponse(UUID id, MemberRole role, Instant joinedAt, String name, String email) {
        this(id, "users", role, joinedAt, name, email);
    }

    public static MemberResponse from(MemberListView view) {
        return new MemberResponse(view.userId(), view.role(), view.joinedAt(), view.name(), view.email());
    }
}
