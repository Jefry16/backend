package com.vointika.page.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/**
 * The page's content — operator-authored raw HTML (Shopify {@code page.content}
 * semantics), rendered only on the operator's own storefront. The trust model
 * is deliberate: the backend stores what the operator wrote — no HTML
 * stripping or sanitization here (escaping is a render/consumer concern, and a
 * stored-mutation would corrupt legitimate markup). The only guards are
 * shape-independent: non-blank, a size cap, and no NUL bytes.
 */
public record PageBody(String value) {

    public static final int MAX_LENGTH = 262_144; // 256 KiB of characters

    public PageBody {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Page body cannot be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidFieldException(
                    "Page body must be at most " + MAX_LENGTH + " characters");
        }
        if (value.indexOf('\u0000') >= 0) {
            throw new InvalidFieldException("Page body contains an invalid character");
        }
    }
}
