package com.vointika.metafield.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

import java.util.Arrays;

/**
 * Which resource kind a definition attaches to. The value machinery is
 * owner-generic, so growing this list is one enum value, one case in
 * {@code MetafieldOwnerAccess}, one controller mount and one CHECK.
 *
 * <p><b>{@code TOUR_OPERATOR} is the operator itself</b> — Shopify's
 * {@code shop.metafields}, and the answer to anything the storefront needs that
 * we deliberately did not model. It is the only owner type whose ownership check
 * needs no other context: the owner and the tenant are the same row.
 *
 * <p>{@code code} is the wire form ("experience"/"page"/"tour_operator");
 * {@code auditEntityType}/{@code action} feed the owner's audit timeline (a
 * metafield value is the OWNER's content, like a translation).
 */
public enum MetafieldOwnerType {

    EXPERIENCE("experience", "EXPERIENCE"),
    PAGE("page", "PAGE"),
    TOUR_OPERATOR("tour_operator", "TOUR_OPERATOR");

    private final String code;
    private final String auditEntityType;

    MetafieldOwnerType(String code, String auditEntityType) {
        this.code = code;
        this.auditEntityType = auditEntityType;
    }

    public String code() { return code; }

    public String auditEntityType() { return auditEntityType; }

    /** "experience.metafield_updated" / "page.metafield_cleared" … */
    public String action(String suffix) { return code + ".metafield_" + suffix; }

    public static MetafieldOwnerType fromCode(String raw) {
        if (raw != null) {
            for (MetafieldOwnerType type : values()) {
                if (type.code.equals(raw)) {
                    return type;
                }
            }
        }
        // Built from the values rather than written out: the message this
        // replaced still said "experience, page" and would have kept saying it.
        throw new InvalidFieldException("Metafield owner type must be one of: "
                + String.join(", ", Arrays.stream(values()).map(MetafieldOwnerType::code).toList()));
    }
}
