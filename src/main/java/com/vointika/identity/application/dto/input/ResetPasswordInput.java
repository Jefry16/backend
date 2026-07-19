package com.vointika.identity.application.dto.input;

public record ResetPasswordInput(
        String token,
        String newPassword
) {}