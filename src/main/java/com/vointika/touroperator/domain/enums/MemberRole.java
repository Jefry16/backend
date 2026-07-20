package com.vointika.touroperator.domain.enums;

/**
 * A team member's role within a tour operator — a trust <b>total order</b>
 * {@code OWNER > ADMIN > STAFF}, each tier a superset of the one below. Fixed
 * set (no custom roles). Persisted by NAME ({@code @Enumerated(STRING)} in a
 * {@code VARCHAR(20)} column), so declaration order is irrelevant to storage —
 * {@link #trustRank} is the authoritative order.
 *
 * <ul>
 *   <li>{@code OWNER} — exactly one per operator (DB-enforced); the creator.</li>
 *   <li>{@code ADMIN} — team + operator profile + everything STAFF can do.</li>
 *   <li>{@code STAFF} — day-to-day operations.</li>
 * </ul>
 */
public enum MemberRole {
    OWNER(3),
    ADMIN(2),
    STAFF(1);

    private final int trustRank;

    MemberRole(int trustRank) {
        this.trustRank = trustRank;
    }

    /**
     * Whether this role is at least as trusted as {@code other} — the tier
     * check the membership policy uses. Reflexive: {@code r.atLeast(r)} is true.
     */
    public boolean atLeast(MemberRole other) {
        return this.trustRank >= other.trustRank;
    }
}
