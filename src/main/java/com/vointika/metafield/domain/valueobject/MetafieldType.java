package com.vointika.metafield.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/**
 * The metafield type catalogue. A definition's type establishes how values
 * are validated and how themes should interpret them. Codes are the wire and
 * storage format ({@code single_line_text}, …). {@code metaobject_reference}
 * stores a metaobject entry id, pinned to one metaobject type on the
 * definition; list and color types are deliberately still out.
 */
public enum MetafieldType {

    SINGLE_LINE_TEXT("single_line_text"),
    MULTI_LINE_TEXT("multi_line_text"),
    NUMBER_INTEGER("number_integer"),
    NUMBER_DECIMAL("number_decimal"),
    BOOLEAN("boolean"),
    DATE("date"),
    URL("url"),
    JSON("json"),
    METAOBJECT_REFERENCE("metaobject_reference");

    private final String code;

    MetafieldType(String code) {
        this.code = code;
    }

    public String code() { return code; }

    /**
     * Whether a per-locale overlay of this type's value means anything.
     *
     * <p><b>Prose only.</b> A translated {@code true} is {@code true} and a
     * translated {@code 2026-08-13} is the same date — those are data, not
     * content. {@code URL} is usually an address rather than content, and a
     * translated {@code JSON} document can carry different keys from the
     * canonical one, so a theme reading {@code value.minHours} would break in one
     * language with nothing to catch it. Both stay out until someone names a
     * case; widening this predicate changes no schema.
     *
     * <p>{@code METAOBJECT_REFERENCE} is not a gap either: pointing at a
     * different entry per locale is content <em>selection</em>, a separate
     * feature. Translating the entry's own fields is how a Spanish boat
     * description happens.
     *
     * <p>It lives on the type because it is a property of the type — the
     * alternative is the same allowlist repeated in three controllers.
     */
    public boolean isTranslatable() {
        return this == SINGLE_LINE_TEXT || this == MULTI_LINE_TEXT;
    }

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
                        + "number_decimal, boolean, date, url, json, metaobject_reference");
    }
}
