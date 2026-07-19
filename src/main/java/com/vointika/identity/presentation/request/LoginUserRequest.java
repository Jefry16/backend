package com.vointika.identity.presentation.request;

public record LoginUserRequest(
        String email,
        String password
) {}