package com.vointika.shared.web.list;

import com.vointika.shared.list.CursorPage;

import java.util.List;
import java.util.function.Function;

public record CursorPageResponse<T>(List<T> data, String nextCursor) {

    public static <S, T> CursorPageResponse<T> of(CursorPage<S> page, Function<S, T> mapper) {
        return new CursorPageResponse<>(
                page.data().stream().map(mapper).toList(),
                page.nextCursor()
        );
    }
}
