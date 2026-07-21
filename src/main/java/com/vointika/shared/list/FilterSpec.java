package com.vointika.shared.list;

import java.util.List;

public record FilterSpec(List<Filter> filters) {

    public static FilterSpec empty() {
        return new FilterSpec(List.of());
    }
}
