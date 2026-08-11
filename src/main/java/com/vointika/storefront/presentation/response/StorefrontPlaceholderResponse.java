package com.vointika.storefront.presentation.response;

/**
 * What every storefront address answers until the themes land.
 *
 * <p>Two fields and both earn it: {@code handle} is the one fact this context
 * still owns — that the host resolved to a storefront that exists — and
 * {@code status} is what stops the body reading as a half-built API to whoever
 * opens the URL. Nothing here is the real page's shape; that comes back with the
 * theme object model (PATTERNS §2a).
 */
public record StorefrontPlaceholderResponse(String handle, String status) {

    public StorefrontPlaceholderResponse(String handle) {
        this(handle, "coming-soon");
    }
}
