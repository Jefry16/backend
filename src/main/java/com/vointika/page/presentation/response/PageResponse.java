package com.vointika.page.presentation.response;

import com.vointika.page.domain.entity.Page;
import com.vointika.page.domain.valueobject.PageSeoDescription;
import com.vointika.page.domain.valueobject.PageSeoTitle;

import java.time.Instant;
import java.util.UUID;

/** A page for read APIs. {@code id} + {@code context:"pages"} per the house rule. */
public record PageResponse(
        UUID id,
        String context,
        String title,
        String handle,
        String body,
        String seoTitle,
        String seoDescription,
        String status,
        String templateSuffix,
        Instant createdAt,
        Instant updatedAt) {

    public static PageResponse from(Page p) {
        return new PageResponse(
                p.getId(),
                "pages",
                p.getTitle().value(),
                p.getHandle().value(),
                p.getBody().value(),
                p.getSeoTitle().map(PageSeoTitle::value).orElse(null),
                p.getSeoDescription().map(PageSeoDescription::value).orElse(null),
                p.getStatus().name(),
                p.getTemplateSuffix(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }
}
