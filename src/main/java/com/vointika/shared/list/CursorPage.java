package com.vointika.shared.list;

import java.util.List;

public record CursorPage<T>(List<T> data, String nextCursor) {

    public static <T> CursorPage<T> empty() {
        return new CursorPage<>(List.of(), null);
    }
}
