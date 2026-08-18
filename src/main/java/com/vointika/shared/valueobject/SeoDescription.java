package com.vointika.shared.valueobject;

/**
 * An SEO meta-description override — 1–320 characters, trimmed, no control or
 * format characters. 320 is what Shopify's admin allows; engines display ~160.
 *
 * <p>Shared for the same reason as {@link SeoTitle}, and replacing the same three
 * per-context copies. Absence is modelled outside this type.
 */
public record SeoDescription(String value) {

    public static final int MAX_LENGTH = 320;

    public SeoDescription {
        value = SeoTextRules.normalize(value, "SEO description", MAX_LENGTH);
    }
}
