package com.vointika.identity.presentation.request;

public record ResetPasswordRequest(
        String token,
        String newPassword
) {}