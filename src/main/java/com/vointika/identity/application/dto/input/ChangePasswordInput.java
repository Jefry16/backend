package com.vointika.identity.application.dto.input;

import java.util.UUID;

public record ChangePasswordInput(
        UUID userId,
        String currentPassword,
        String newPassword
) {}