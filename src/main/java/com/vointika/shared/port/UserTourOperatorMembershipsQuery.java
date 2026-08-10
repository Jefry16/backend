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
     * {@code timezone} and {@code currency} are the <b>resolved</b> reference
     * values, not the ids the operator row stores — the IANA name
     * ({@code "Europe/Madrid"}) and the ISO 4217 code ({@code "EUR"}). Both are
     * non-null: {@code timezone_id} and {@code currency_id} are NOT NULL FKs into
     * {@code reference}, so every membership resolves both.
     *
     * <p>The currency is the <b>code</b> and deliberately not the symbol:
     * {@code Intl.NumberFormat} takes a code and derives the symbol <em>and</em>
     * the right number of decimals per locale (two for EUR, none for JPY). A
     * symbol throws that away. It rides the profile for the same reason the
     * timezone does — the admin formats money on every experience and slot
     * screen, and the alternative is fetching the operator plus the whole
     * currency reference list and joining them in the client, which renders
     * {@code 95.00} and then flips to {@code €95.00} when the list lands.
     *
     * <p>{@code role} is the caller's role in THIS operator — a {@code MemberRole}
     * name ({@code "OWNER"}/{@code "ADMIN"}/{@code "STAFF"}), a primitive so the
     * enum never crosses into {@code shared} (§4.2). Non-null: every membership
     * has a role. It rides the profile summary so the admin SPA can gate UI on
     * the caller's per-operator role without an extra fetch.
     */
    record TourOperatorMembershipView(UUID id, String name, String logoUrl, String timezone,
                                      String currency, boolean isDefault, String role) {}
}
