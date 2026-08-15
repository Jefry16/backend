package com.vointika.shared.list;

import java.util.Optional;

public enum FilterOp {
    EQ("eq"),
    NEQ("neq"),
    CONTAINS("contains"),
    NOT_CONTAINS("not_contains"),
    STARTS_WITH("starts_with"),
    ENDS_WITH("ends_with"),
    IN("in"),
    NOT_IN("not_in"),
    GT("gt"),
    GTE("gte"),
    LT("lt"),
    LTE("lte"),
    BETWEEN("between");

    private final String token;

    FilterOp(String token) {
        this.token = token;
    }

    public String token() {
        return token;
    }

    /**
     * Ops built as a negation, and therefore the ones a null column breaks.
     *
     * <p>{@code WHERE NOT (col LIKE 'x')} is UNKNOWN when {@code col} is null,
     * and {@code WHERE} keeps only true rows — so a row with no value matches
     * neither the filter nor its negation, and silently vanishes from the
     * result. Measured before this existed: 12 contact messages, one nameless,
     * and {@code not_contains=zzz} returned 11. A filter that excludes nothing
     * still lost a row.
     *
     * <p>The positive ops have no such problem: a null genuinely does not
     * contain "x", so dropping it is the right answer.
     */
    public boolean isNegating() {
        return this == NEQ || this == NOT_CONTAINS || this == NOT_IN;
    }

    public static Optional<FilterOp> fromToken(String token) {
        for (FilterOp op : values()) {
            if (op.token.equals(token)) {
                return Optional.of(op);
            }
        }
        return Optional.empty();
    }
}
