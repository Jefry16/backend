package com.vointika.identity.presentation.request;

public record ChangePasswordRequest(
        String currentPassword,
        String newPassword
) {}