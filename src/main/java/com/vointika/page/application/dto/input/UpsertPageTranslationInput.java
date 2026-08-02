package com.vointika.page.application.dto.input;

import java.util.UUID;

public record UpsertPageTranslationInput(
        UUID callerUserId,
        UUID tourOperatorId,
        UUID pageId,
        String locale,
        String title,
        String body,
        String seoTitle,
        String seoDescription,
        String handle) {
}
