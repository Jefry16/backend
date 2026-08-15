package com.vointika.shared.list;


import java.time.Instant;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class ListSchema {

    private final Map<String, FilterFieldDef> filters;
    private final Set<String> nullableFilters;
    private final Set<String> sortable;
    private final SortSpec defaultSort;
    private final boolean tenantScoped;

    private ListSchema(Builder b) {
        this.filters = Map.copyOf(b.filters);
        this.nullableFilters = Set.copyOf(b.nullableFilters);
        this.sortable = Set.copyOf(b.sortable);
        this.defaultSort = b.defaultSort;
        this.tenantScoped = b.tenantScoped;
    }

    public Map<String, FilterFieldDef> filters() { return filters; }

    /**
     * Filterable fields whose column can be null, which is what makes a
     * negating operator unanswerable on them (see {@link FilterOp#isNegating()}).
     *
     * <p><b>Why the schema carries a fact about the database.</b> The parser is
     * where the refusal has to happen — it is the only place that sees the
     * operator the caller asked for — and it sees nothing but this schema. The
     * alternative was to reflect on the JPA entity inside the executor, which
     * moves the error later and does it per request. The schema already declares
     * each field's value type, so its nullability is the same kind of fact.
     *
     * <p>It is a declaration, so it can be wrong.
     * {@code FilterableNullableColumnsAreDeclaredTest} checks it against the
     * entity in <b>both</b> directions: a field declared here that is really
     * {@code NOT NULL} needlessly refuses a working filter, and a nullable
     * column missing from here is the silent-row-loss bug this exists to stop.
     */
    public Set<String> nullableFilters() { return nullableFilters; }

    public Set<String> sortable() { return sortable; }
    public SortSpec defaultSort() { return defaultSort; }
    public boolean tenantScoped() { return tenantScoped; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, FilterFieldDef> filters = new HashMap<>();
        private final Set<String> nullableFilters = new LinkedHashSet<>();
        private final Set<String> sortable = new LinkedHashSet<>();
        private SortSpec defaultSort;
        private boolean tenantScoped;

        public Builder text(String field) {
            filters.put(field, new FilterFieldDef(FilterType.TEXT, String.class));
            return this;
        }

        public Builder set(String field, Class<?> valueType) {
            filters.put(field, new FilterFieldDef(FilterType.SET, valueType));
            return this;
        }

        public Builder number(String field, Class<?> valueType) {
            filters.put(field, new FilterFieldDef(FilterType.NUMBER, valueType));
            return this;
        }

        public Builder time(String field) {
            filters.put(field, new FilterFieldDef(FilterType.TIME, LocalTime.class));
            return this;
        }

        public Builder instant(String field) {
            filters.put(field, new FilterFieldDef(FilterType.INSTANT, Instant.class));
            return this;
        }

        public Builder bool(String field) {
            filters.put(field, new FilterFieldDef(FilterType.BOOLEAN, Boolean.class));
            return this;
        }

        /**
         * Marks an already-declared filter field as backed by a nullable column,
         * which makes a negating operator on it a 422 rather than a short list.
         *
         * <p>Order-independent by design — it names the field rather than
         * modifying "the last one added", so a reordered builder chain cannot
         * quietly move the marker onto a different field.
         */
        public Builder nullable(String field) {
            nullableFilters.add(field);
            return this;
        }

        public Builder sortable(String field) {
            sortable.add(field);
            return this;
        }

        public Builder defaultSort(String token) {
            this.defaultSort = SortSpec.parse(token);
            return this;
        }

        public Builder tenantScoped() {
            this.tenantScoped = true;
            return this;
        }

        public ListSchema build() {
            if (defaultSort == null) {
                throw new IllegalStateException("defaultSort is required");
            }
            if (!sortable.contains(defaultSort.field())) {
                throw new IllegalStateException(
                        "defaultSort field '" + defaultSort.field() + "' is not in sortable set");
            }
            // A marker on a field nobody declared protects nothing, and reads as
            // though it does — the failure mode of every allowlist with a typo in it.
            for (String field : nullableFilters) {
                if (!filters.containsKey(field)) {
                    throw new IllegalStateException(
                            "nullable('" + field + "') names no declared filter field");
                }
            }
            // Sorting on a nullable column loses rows after page one rather than
            // returning a short answer, so it is refused outright rather than
            // being made to answer. SortableColumnsAreNeverNullableTest catches
            // the case where the column is nullable but nobody declared it here.
            for (String field : sortable) {
                if (nullableFilters.contains(field)) {
                    throw new IllegalStateException(
                            "'" + field + "' is declared nullable and sortable; a keyset cursor "
                                    + "cannot page over null sort keys");
                }
            }
            return new ListSchema(this);
        }

    }
}
