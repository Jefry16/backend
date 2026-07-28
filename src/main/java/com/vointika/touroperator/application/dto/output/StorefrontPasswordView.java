package com.vointika.touroperator.application.dto.output;

/**
 * The operator's storefront password-protection settings. The password is
 * member-visible by design — it's the shared gate the operator hands out
 * (Shopify shows it in the admin the same way), not a credential.
 */
public record StorefrontPasswordView(boolean enabled, String password, String message) {
}
