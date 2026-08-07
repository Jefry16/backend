package com.vointika.touroperator.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/**
 * The policy document itself — <b>raw HTML, stored verbatim</b>, exactly as
 * {@code PageBody} is and with the same limits, because it is the same kind of
 * content written by the same author for the same site.
 *
 * <p><b>Nothing here escapes or sanitises.</b> The storefront renders this
 * unescaped ({@code {{{policy.body}}}}), which is a deliberate trust boundary
 * recorded under MAP open decision 6: it is <em>our</em> template rendering
 * <em>their</em> content, the boundary Shopify's own policy pages sit on.
 * Escaping here would ship {@code &lt;p&gt;} to every visitor. That is a
 * different question from running a template <em>they wrote</em>, which is what
 * the engine choice guards against.
 *
 * <p>What is rejected is what a database or a parser cannot hold: a NUL byte
 * (Postgres {@code TEXT} cannot store one) and a document past the cap.
 */
public record PolicyBody(String value) {

    public static final int MAX_LENGTH = 262_144; // 256 KiB of characters, as PageBody

    public PolicyBody {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Policy body cannot be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidFieldException(
                    "Policy body must be at most " + MAX_LENGTH + " characters");
        }
        if (value.indexOf('\u0000') >= 0) {
            throw new InvalidFieldException("Policy body contains an invalid character");
        }
    }
}
