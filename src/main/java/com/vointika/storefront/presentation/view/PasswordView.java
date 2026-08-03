package com.vointika.storefront.presentation.view;

import com.vointika.storefront.application.dto.output.PasswordPageOutput;

/**
 * The Mustache context object for the password page.
 *
 * <p><b>It carries no trace of the password.</b> The value exists to be compared
 * inside the gate; a view model that could reach it is a plaintext password on
 * the one page every anonymous visitor sees.
 *
 * <p><b>Must stay public, and so must anything nested in it.</b> The compiler
 * runs with access coercion off, so {@code Method.invoke} on a package-private
 * class throws {@code IllegalAccessException} even when the accessor itself is
 * public — at render time, not compile time.
 */
public record PasswordView(String shopName, String message, boolean error) {

    public static PasswordView from(PasswordPageOutput page, boolean error) {
        return new PasswordView(page.shopName(), page.message(), error);
    }
}
