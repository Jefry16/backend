package com.vointika.identity.application.dto.input;

public record RegisterUserInput(
        String email,
        String name,
        String password,
        /** Optional: the frontend's current UI language (locale code). */
        String language
) {}