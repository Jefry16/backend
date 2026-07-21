package com.vointika.shared.infrastructure.list;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.list.SortDirection;
import com.vointika.shared.list.SortSpec;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CursorCodecTest {

    private final SortSpec sort = new SortSpec("createdAt", SortDirection.DESC);
    private final UUID id = UUID.fromString("018f8c00-0000-7000-8000-000000000001");

    @Test
    void roundTripPreservesSortFieldDirectionValueAndId() {
        String cursor = CursorCodec.encode(sort, "2026-04-25T12:00:00Z", id);

        CursorCodec.Decoded decoded = CursorCodec.decode(cursor, sort);

        assertThat(decoded.sortField()).isEqualTo("createdAt");
        assertThat(decoded.direction()).isEqualTo(SortDirection.DESC);
        assertThat(decoded.sortValue()).isEqualTo("2026-04-25T12:00:00Z");
        assertThat(decoded.id()).isEqualTo(id);
    }

    @Test
    void preservesValueWithSeparatorCharacters() {
        String value = "name|with.dots|and|pipes";
        String cursor = CursorCodec.encode(new SortSpec("name", SortDirection.ASC), value, id);

        CursorCodec.Decoded decoded = CursorCodec.decode(cursor, new SortSpec("name", SortDirection.ASC));

        assertThat(decoded.sortValue()).isEqualTo(value);
    }

    @Test
    void rejectsCursorWithMismatchedSortField() {
        String cursor = CursorCodec.encode(sort, "x", id);

        assertThatThrownBy(() -> CursorCodec.decode(cursor, new SortSpec("name", SortDirection.DESC)))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("Cursor does not match");
    }

    @Test
    void rejectsCursorWithMismatchedDirection() {
        String cursor = CursorCodec.encode(sort, "x", id);

        assertThatThrownBy(() -> CursorCodec.decode(cursor, new SortSpec("createdAt", SortDirection.ASC)))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("Cursor does not match");
    }

    @Test
    void rejectsMalformedCursor() {
        assertThatThrownBy(() -> CursorCodec.decode("!!!not-base64!!!", sort))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("Invalid cursor");
    }
}
