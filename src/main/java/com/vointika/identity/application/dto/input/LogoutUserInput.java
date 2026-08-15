package com.vointika.identity.application.dto.input;

import java.util.UUID;

public record LogoutUserInput(UUID userId, String refreshToken) {}
