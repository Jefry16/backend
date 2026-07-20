package com.vointika.touroperator.presentation.response;

import com.vointika.touroperator.application.dto.output.MemberListView;
import com.vointika.touroperator.domain.enums.MemberRole;

import java.time.Instant;
import java.util.UUID;

/**
 * One team-roster row. {@code name}/{@code email} are best-effort from identity
 * (null if the account can't be resolved). Carries the {@code "members"} type
 * discriminator (the two-constructor pattern).
 */
public record MemberResponse(
        UUID userId,
        String type,
        MemberRole role,
        Instant joinedAt,
        String name,
        String email) {

    public MemberResponse(UUID userId, MemberRole role, Instant joinedAt, String name, String email) {
        this(userId, "members", role, joinedAt, name, email);
    }

    public static MemberResponse from(MemberListView view) {
        return new MemberResponse(view.userId(), view.role(), view.joinedAt(), view.name(), view.email());
    }
}
