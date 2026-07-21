package com.vointika.shared.infrastructure.list;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.list.SortDirection;
import com.vointika.shared.list.SortSpec;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public final class CursorCodec {

    private static final String VERSION = "v1";

    public record Decoded(String sortField, SortDirection direction, String sortValue, UUID id) {}

    public static String encode(SortSpec sort, String sortValueStr, UUID id) {
        char dir = sort.direction() == SortDirection.DESC ? 'D' : 'A';
        String sortValueB64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(sortValueStr.getBytes(StandardCharsets.UTF_8));
        String inner = VERSION + "." + sort.field() + "." + dir + "." + sortValueB64 + "." + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(inner.getBytes(StandardCharsets.UTF_8));
    }

    public static Decoded decode(String cursor, SortSpec expectedSort) {
        try {
            byte[] outerBytes = Base64.getUrlDecoder().decode(cursor);
            String inner = new String(outerBytes, StandardCharsets.UTF_8);
            String[] parts = inner.split("\\.", 5);
            if (parts.length != 5 || !VERSION.equals(parts[0])) {
                throw badCursor();
            }
            String sortField = parts[1];
            SortDirection direction = switch (parts[2]) {
                case "D" -> SortDirection.DESC;
                case "A" -> SortDirection.ASC;
                default -> throw badCursor();
            };
            String sortValue = new String(
                    Base64.getUrlDecoder().decode(parts[3]),
                    StandardCharsets.UTF_8
            );
            UUID id = UUID.fromString(parts[4]);

            if (!sortField.equals(expectedSort.field()) || direction != expectedSort.direction()) {
                throw new InvalidFieldException("Cursor does not match the requested sort");
            }
            return new Decoded(sortField, direction, sortValue, id);
        } catch (IllegalArgumentException e) {
            throw badCursor();
        }
    }

    private static InvalidFieldException badCursor() {
        return new InvalidFieldException("Invalid cursor");
    }

    private CursorCodec() {}
}
