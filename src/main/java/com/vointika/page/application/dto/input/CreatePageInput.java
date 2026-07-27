package com.vointika.page.application.dto.input;

import java.util.UUID;

public record CreatePageInput(
        UUID callerUserId,
        UUID tourOperatorId,
        String title,
        String handle,
        String body,
        String seoTitle,
        String seoDescription) {
}
