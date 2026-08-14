package com.vointika.identity.application.dto.input;

import java.io.InputStream;

import java.util.UUID;

public record SetAvatarInput(
        UUID userId,
        String contentType,
        long sizeBytes,
        InputStream body
) {}
