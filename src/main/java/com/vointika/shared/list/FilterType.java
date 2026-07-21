package com.vointika.shared.list;

import java.util.Set;

public enum FilterType {
    TEXT(Set.of(
            FilterOp.EQ,
            FilterOp.NEQ,
            FilterOp.CONTAINS,
            FilterOp.NOT_CONTAINS,
            FilterOp.STARTS_WITH,
            FilterOp.ENDS_WITH
    )),
    SET(Set.of(
            FilterOp.IN,
            FilterOp.NOT_IN
    )),
    NUMBER(Set.of(
            FilterOp.EQ,
            FilterOp.NEQ,
            FilterOp.GT,
            FilterOp.GTE,
            FilterOp.LT,
            FilterOp.LTE,
            FilterOp.BETWEEN
    )),
    DATE(Set.of(
            FilterOp.EQ,
            FilterOp.NEQ,
            FilterOp.LT,
            FilterOp.GT,
            FilterOp.BETWEEN
    )),
    TIME(Set.of(
            FilterOp.EQ,
            FilterOp.NEQ,
            FilterOp.LT,
            FilterOp.GT,
            FilterOp.BETWEEN
    )),
    // Timestamp (java.time.Instant). Full comparable set incl. GTE/LTE for
    // inclusive ranges — the natural shape for "between these two moments".
    INSTANT(Set.of(
            FilterOp.EQ,
            FilterOp.NEQ,
            FilterOp.GT,
            FilterOp.GTE,
            FilterOp.LT,
            FilterOp.LTE,
            FilterOp.BETWEEN
    )),
    BOOLEAN(Set.of(
            FilterOp.EQ,
            FilterOp.NEQ
    ));

    private final Set<FilterOp> allowedOps;

    FilterType(Set<FilterOp> allowedOps) {
        this.allowedOps = allowedOps;
    }

    public Set<FilterOp> allowedOps() {
        return allowedOps;
    }
}
