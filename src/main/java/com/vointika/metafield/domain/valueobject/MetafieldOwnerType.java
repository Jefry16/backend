package com.vointika.metafield.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/**
 * Which resource kind a definition attaches to. Both v1 owner types ship
 * together (unlike the archive, which grew PAGE later): the rebuild has both
 * contexts, so the value machinery is owner-generic from day one.
 *
 * <p>{@code code} is the wire form ("experience"/"page");
 * {@code auditEntityType}/{@code actionPrefix} feed the owner's audit
 * timeline (a metafield value is the OWNER's content, like a translation).
 */
public enum MetafieldOwnerType {

    EXPERIENCE("experience", "EXPERIENCE"),
    PAGE("page", "PAGE");

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
        throw new InvalidFieldException("Metafield owner type must be one of: experience, page");
    }
}
