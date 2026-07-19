package com.vointika.identity.presentation.request;

public record RegisterUserRequest(
        String email,
        String name,
        String password
) {}