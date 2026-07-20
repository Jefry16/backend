package com.vointika.identity.presentation.request;

public record RegisterUserRequest(
        String email,
        String name,
        String password,
        /** Optional: the frontend's current UI language (locale code). Stored as the
         *  new user's preference and used to localize the verification email. */
        String language
) {}