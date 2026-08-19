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
     * <b>The tenant-isolation answer, in one place.</b> Four causes say it — the
     * operator does not exist, you are not a member of it, the id in the URI is
     * malformed, and there is no authenticated principal.
     *
     * <p>The two that matter for enumeration are indistinguishable <b>by structure</b>:
     * {@link #ensureMember}'s implementation throws once, behind a predicate that is
     * false for both. This constant does not create that property — it means the
     * sentence can be changed in one place rather than nineteen.
     *
     * <p>It was written out as a literal <b>twenty times across nineteen files</b>,
     * plus once in {@code metafield}. Nothing made them agree, so a single reworded
     * copy would have leaked the isolation with a green build.
     * {@code TenantNotFoundMessageIsWrittenOnceTest} fails the build if the sentence
     * reappears as a literal.
     */
    String TENANT_NOT_FOUND = "Tour operator not found";

    /**
     * The 403 an insufficient tier gets, built the way the policy builds it.
     *
     * <p><b>The one published message this series could not collapse the usual way</b>,
     * because production <em>interpolates</em> it — {@code "This action requires " +
     * minimum + " privileges"} — so there was no constant to point at and fifteen test
     * files across seven contexts spelled the sentence out instead, eight of them
     * publishing it.
     *
     * <p>That is also why no per-context pass ever fixed it: {@code media}'s pass owns
     * three of the fifteen, {@code touroperator}'s four, and the sentence only becomes
     * one fact if something outside all of them owns it. A worklist item that no pass
     * owns is one that never gets done.
     *
     * <p>Takes the role's name rather than {@code MemberRole}, which is
     * {@code touroperator}'s enum and cannot cross into {@code shared}.
     */
    static String requiresRoleMessage(String roleName) {
        return "This action requires " + roleName + " privileges";
    }

    /**
     * Throws {@link com.vointika.shared.exception.ResourceNotFoundException}
     * ({@link #TENANT_NOT_FOUND}) if the user is not a member of the operator —
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
