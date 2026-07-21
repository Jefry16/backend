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

    public static Optional<FilterOp> fromToken(String token) {
        for (FilterOp op : values()) {
            if (op.token.equals(token)) {
                return Optional.of(op);
            }
        }
        return Optional.empty();
    }
}
