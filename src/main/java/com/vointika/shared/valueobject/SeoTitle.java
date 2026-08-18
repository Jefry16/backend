package com.vointika.shared.valueobject;

/**
 * An SEO {@code <title>} override — 1–70 characters, trimmed, no control or
 * format characters.
 *
 * <p><b>Shared because the width is SERP truncation, not a per-context choice.</b>
 * Three contexts overrode a title — an experience, a page, and the operator itself
 * as the fallback for both — and each had its own record with the same limit and
 * the same checks. Two of the three were byte-identical apart from their javadoc;
 * the third differed only by prefixing {@code "Page "} onto each refusal.
 *
 * <p>Absence is modelled outside this type: an owner holds no override rather than
 * a blank one, which is why nothing here accepts null.
 */
public record SeoTitle(String value) {

    public static final int MAX_LENGTH = 70;

    public SeoTitle {
        value = SeoTextRules.normalize(value, "SEO title", MAX_LENGTH);
    }
}
