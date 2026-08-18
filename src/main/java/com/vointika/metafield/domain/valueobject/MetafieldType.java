package com.vointika.metafield.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

import java.util.Arrays;
import java.util.stream.Collectors;

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

    /**
     * Every code, in declaration order — <b>derived, because it is published</b>.
     * The refusal below and the create endpoint's {@code type} description are both
     * built from this. The list used to be written out by hand in both places, and
     * this enum is expected to grow (see the class javadoc), so a tenth type would
     * have been accepted while both published copies still named nine.
     * {@code MetafieldOwnerType} has always derived its own; this now matches it.
     */
    public static String codes() {
        return Arrays.stream(values()).map(MetafieldType::code)
                .collect(Collectors.joining(", "));
    }

    public static MetafieldType fromCode(String raw) {
        if (raw != null) {
            for (MetafieldType type : values()) {
                if (type.code.equals(raw)) {
                    return type;
                }
            }
        }
        throw new InvalidFieldException("Metafield type must be one of: " + codes());
    }

    /**
     * Whether a <em>metaobject field</em> may be declared with this type.
     *
     * <p>Nested references are out for now — a metaobject field pointing at another
     * metaobject is a shape nothing needs, and the reference type belongs to
     * experience/page metafields. The rule lived as {@code == METAOBJECT_REFERENCE}
     * in two use cases and as a hand-written eight-code list in a published field
     * description; it is one predicate now, so widening it moves the refusal and
     * the guide together.
     */
    public boolean allowedAsMetaobjectField() {
        return this != METAOBJECT_REFERENCE;
    }

    /**
     * The refusal, with the excluded set derived rather than spelled — the predicate
     * above is written as a predicate so a second type can be excluded, and a
     * hardcoded name would keep publishing only the first.
     */
    public static String notAMetaobjectFieldTypeMessage() {
        String excluded = Arrays.stream(values()).filter(t -> !t.allowedAsMetaobjectField())
                .map(MetafieldType::code).collect(Collectors.joining(", "));
        return "Metaobject fields cannot use the " + excluded + " type";
    }

    /** The codes a metaobject field may declare — derived, because it is published. */
    public static String metaobjectFieldCodes() {
        return Arrays.stream(values()).filter(MetafieldType::allowedAsMetaobjectField)
                .map(MetafieldType::code).collect(Collectors.joining(", "));
    }

    /**
     * The codes a per-locale overlay is accepted for, derived from
     * {@link #isTranslatable()} so widening the predicate moves what is published.
     * The guide stated this set in prose until it was found to be a fourth copy of
     * the same class of defect.
     */
    public static String translatableCodes() {
        return Arrays.stream(values()).filter(MetafieldType::isTranslatable)
                .map(MetafieldType::code).collect(Collectors.joining(", "));
    }
}
