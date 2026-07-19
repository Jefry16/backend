package com.vointika.identity.application.dto.input;

public record LoginUserInput(
        String email,
        String password
) {}