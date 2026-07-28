package com.vointika.touroperator.presentation.response;

import com.vointika.touroperator.application.dto.output.StorefrontPasswordView;

/** Settings sub-resource shape (like /locales) — no id/context envelope. */
public record StorefrontPasswordResponse(boolean enabled, String password, String message) {

    public static StorefrontPasswordResponse from(StorefrontPasswordView view) {
        return new StorefrontPasswordResponse(view.enabled(), view.password(), view.message());
    }
}
