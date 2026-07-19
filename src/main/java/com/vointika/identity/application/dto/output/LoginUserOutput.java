package com.vointika.identity.application.dto.output;

public record LoginUserOutput(
        String accessToken,
        String refreshToken
) {}