package com.vointika.identity.application.dto.input;

public record RegisterUserInput(
        String email,
        String name,
        String password
) {}