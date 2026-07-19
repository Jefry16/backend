package com.vointika.shared.port;

import java.util.List;
import java.util.UUID;

/**
 * Returns the tour operators a given user is a member of, ordered by default
 * membership first then by name. Used to render the user's operator switcher
 * in the profile response.
 *
 * <p>Owned by the {@code touroperator} context per §3.5 (cross-context reads
 * go through shared query ports).
 */
public interface UserTourOperatorMembershipsQuery {

    List<TourOperatorMembershipView> findForUser(UUID userId);

    /**
     * {@code role} is the caller's role in THIS operator — a {@code MemberRole}
     * name ({@code "OWNER"}/{@code "ADMIN"}/{@code "STAFF"}), a primitive so the
     * enum never crosses into {@code shared} (§4.2). Non-null: every membership
     * has a role. It rides the profile summary so the admin SPA can gate UI on
     * the caller's per-operator role without an extra fetch.
     */
    record TourOperatorMembershipView(UUID id, String name, String logoUrl, String timezone,
                                      boolean isDefault, String role) {}
}
