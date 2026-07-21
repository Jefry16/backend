package com.vointika.shared.list;


import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class ListSchema {

    private final Map<String, FilterFieldDef> filters;
    private final Set<String> sortable;
    private final SortSpec defaultSort;
    private final boolean tenantScoped;

    private ListSchema(Builder b) {
        this.filters = Map.copyOf(b.filters);
        this.sortable = Set.copyOf(b.sortable);
        this.defaultSort = b.defaultSort;
        this.tenantScoped = b.tenantScoped;
    }

    public Map<String, FilterFieldDef> filters() { return filters; }
    public Set<String> sortable() { return sortable; }
    public SortSpec defaultSort() { return defaultSort; }
    public boolean tenantScoped() { return tenantScoped; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, FilterFieldDef> filters = new HashMap<>();
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

        public Builder date(String field) {
            filters.put(field, new FilterFieldDef(FilterType.DATE, LocalDate.class));
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

        public Builder sortable(String field) {
            sortable.add(field);
            return this;
        }

        public Builder defaultSort(String token) {
            this.defaultSort = parseSortToken(token);
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
            return new ListSchema(this);
        }

        private static SortSpec parseSortToken(String token) {
            if (token == null || token.isEmpty()) {
                throw new IllegalArgumentException("sort token must not be empty");
            }
            if (token.charAt(0) == '-') {
                return new SortSpec(token.substring(1), SortDirection.DESC);
            }
            return new SortSpec(token, SortDirection.ASC);
        }
    }
}
