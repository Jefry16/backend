package com.vointika.shared.port;

import java.util.UUID;

/**
 * Shared-kernel port for verifying a user's membership/role in a tour operator,
 * so any bounded context can enforce the rule without importing from
 * {@code touroperator}. The adapter lives in {@code touroperator.application.policy};
 * this port stays role-agnostic (no {@code MemberRole} in {@code shared}, PATTERNS §6).
 */
public interface TourOperatorMembershipCheck {

    /**
     * Throws {@link com.vointika.shared.exception.ResourceNotFoundException}
     * ("Tour operator not found") if the user is not a member of the operator —
     * or the operator does not exist. Identical responses preserve tenant isolation.
     */
    void ensureMember(UUID userId, UUID tourOperatorId);

    /**
     * The user must be at least ADMIN. An insufficient tier (STAFF) — or a caller
     * with no resolvable role — gets a {@link com.vointika.shared.exception.ForbiddenException}
     * (403), distinct from the non-member 404 the interceptor raises.
     */
    void ensureAdmin(UUID userId, UUID tourOperatorId);

    /**
     * The user must be the OWNER. A non-owner member (ADMIN/STAFF) — or an
     * unresolvable role — gets a {@link com.vointika.shared.exception.ForbiddenException} (403).
     */
    void ensureOwner(UUID userId, UUID tourOperatorId);
}
