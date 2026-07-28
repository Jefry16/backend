package com.vointika.touroperator.presentation.request;

public record UpdateStorefrontPasswordRequest(boolean enabled, String password, String message) {
}
