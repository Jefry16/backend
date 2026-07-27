package com.vointika.metafield.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/**
 * The v1 metafield type catalogue. A definition's type establishes how values
 * are validated and how themes should interpret them. Codes are the wire and
 * storage format ({@code single_line_text}, …); reference, list and color
 * types are deliberately out of v1.
 */
public enum MetafieldType {

    SINGLE_LINE_TEXT("single_line_text"),
    MULTI_LINE_TEXT("multi_line_text"),
    NUMBER_INTEGER("number_integer"),
    NUMBER_DECIMAL("number_decimal"),
    BOOLEAN("boolean"),
    DATE("date"),
    URL("url"),
    JSON("json");

    private final String code;

    MetafieldType(String code) {
        this.code = code;
    }

    public String code() { return code; }

    public static MetafieldType fromCode(String raw) {
        if (raw != null) {
            for (MetafieldType type : values()) {
                if (type.code.equals(raw)) {
                    return type;
                }
            }
        }
        throw new InvalidFieldException(
                "Metafield type must be one of: single_line_text, multi_line_text, number_integer, "
                        + "number_decimal, boolean, date, url, json");
    }
}
