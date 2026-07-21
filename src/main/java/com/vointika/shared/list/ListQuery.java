package com.vointika.shared.list;

import java.util.UUID;

public record ListQuery(
        UUID tenantId,
        FilterSpec filters,
        SortSpec sort,
        String cursor
) {
}
