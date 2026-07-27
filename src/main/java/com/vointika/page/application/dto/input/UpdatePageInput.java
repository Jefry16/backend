package com.vointika.page.application.dto.input;

import java.util.UUID;

public record UpdatePageInput(
        UUID callerUserId,
        UUID tourOperatorId,
        UUID pageId,
        String title,
        String body,
        String seoTitle,
        String seoDescription,
        String templateSuffix) {
}
