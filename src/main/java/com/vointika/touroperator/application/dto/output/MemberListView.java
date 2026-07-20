package com.vointika.touroperator.application.dto.output;

import com.vointika.touroperator.domain.enums.MemberRole;

import java.time.Instant;
import java.util.UUID;

/**
 * One team-roster row: the membership facts plus the identity-resolved
 * {@code name}/{@code email}. Those two are batch-resolved best-effort — an
 * account that can no longer be resolved carries null rather than dropping the row.
 */
public record MemberListView(
        UUID userId,
        MemberRole role,
        Instant joinedAt,
        String name,
        String email) {
}
