package com.vointika.shared.list;

public record SortSpec(String field, SortDirection direction) {

    /**
     * The one parser for the {@code -field} sort grammar.
     *
     * <p>There were two, with different rules for the same token: this one
     * rejected a bare {@code "-"}, the {@code ListSchema.Builder} copy accepted
     * it and produced {@code SortSpec("", DESC)}. That failed safe one step later
     * — {@code build()} throws because {@code ""} is not in the sortable set — but
     * two copies of a grammar drift, and the safety net was incidental rather
     * than intended.
     *
     * <p>It throws {@link IllegalArgumentException} rather than the web layer's
     * {@code InvalidFieldException} because {@code shared.list} is below the web
     * layer and cannot see it. The caller that parses <b>user input</b> catches
     * and translates; the caller that parses a <b>hardcoded default</b> lets it
     * fly, which is correct — a bad default is a programming error and belongs at
     * startup, not in a 422 to whoever happened to call the endpoint first.
     */
    public static SortSpec parse(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("sort token must not be empty");
        }
        if (token.charAt(0) == '-') {
            String field = token.substring(1);
            if (field.isEmpty()) {
                throw new IllegalArgumentException("Invalid sort: missing field");
            }
            return new SortSpec(field, SortDirection.DESC);
        }
        return new SortSpec(token, SortDirection.ASC);
    }
}
