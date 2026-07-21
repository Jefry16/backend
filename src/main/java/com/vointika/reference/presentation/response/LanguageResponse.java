package com.vointika.reference.presentation.response;

import java.util.UUID;

public record LanguageResponse(
        UUID id,
        String context,
        String code,
        String name
) {
    public LanguageResponse(UUID id, String code, String name) {
        this(id, "languages", code, name);
    }
}
