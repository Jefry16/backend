package com.vointika.storefront.application.dto.output;

/**
 * The password page's copy. It carries no locale: the gate answers before the
 * locale rule runs, so the message is the operator's primary-locale one and the
 * page has no other locale to be in.
 *
 * <p>The password itself is deliberately absent — it is compared inside the
 * application layer and never leaves it.
 */
public record PasswordPageOutput(String shopName, String message) {}
