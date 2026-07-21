package com.vointika.shared.list;

import java.util.Map;
import java.util.UUID;

public record ListQuery(
        UUID tenantId,
        FilterSpec filters,
        SortSpec sort,
        String cursor,
        Map<String, Object> scope
) {

    public ListQuery(UUID tenantId, FilterSpec filters, SortSpec sort, String cursor) {
        this(tenantId, filters, sort, cursor, Map.of());
    }

    public ListQuery withScope(Map<String, Object> newScope) {
        return new ListQuery(tenantId, filters, sort, cursor, newScope);
    }
}
