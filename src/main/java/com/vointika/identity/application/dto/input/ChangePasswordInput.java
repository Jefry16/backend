package com.vointika.identity.application.dto.input;

public record ChangePasswordInput(
        String userId,
        String currentPassword,
        String newPassword
) {}