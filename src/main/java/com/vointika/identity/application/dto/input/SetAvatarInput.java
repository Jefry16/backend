package com.vointika.identity.application.dto.input;

import java.io.InputStream;

public record SetAvatarInput(
        String userId,
        String contentType,
        long sizeBytes,
        InputStream body
) {}
