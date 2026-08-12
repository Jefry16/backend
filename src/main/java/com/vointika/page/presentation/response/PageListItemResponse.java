package com.vointika.page.presentation.response;

import com.vointika.page.domain.projection.PageListItem;

import java.time.Instant;
import java.util.UUID;

/** A pages-list row — no {@code body} (heavy HTML stays on the detail read). */
public record PageListItemResponse(
        UUID id,
        String context,
        String title,
        String handle,
        boolean published,
        Instant createdAt,
        Instant updatedAt) {

    public static PageListItemResponse from(PageListItem item) {
        return new PageListItemResponse(
                item.id(),
                "pages",
                item.title(),
                item.handle(),
                item.published(),
                item.createdAt(),
                item.updatedAt());
    }
}
