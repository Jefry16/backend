package com.vointika.page.presentation.response;

import com.vointika.page.domain.entity.Page;
import com.vointika.shared.valueobject.SeoDescription;
import com.vointika.shared.valueobject.SeoTitle;

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
        boolean published,
        Instant createdAt,
        Instant updatedAt) {

    public static PageResponse from(Page p) {
        return new PageResponse(
                p.getId(),
                "pages",
                p.getTitle().value(),
                p.getHandle().value(),
                p.getBody().value(),
                p.getSeoTitle().map(SeoTitle::value).orElse(null),
                p.getSeoDescription().map(SeoDescription::value).orElse(null),
                p.isPublished(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }
}
