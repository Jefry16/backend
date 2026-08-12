package com.vointika.page.domain.projection;


import java.time.Instant;
import java.util.UUID;

/** A pages-list row — deliberately WITHOUT {@code body} (heavy HTML stays on the detail read). */
public record PageListItem(
        UUID id,
        String title,
        String handle,
        boolean published,
        Instant createdAt,
        Instant updatedAt
) {}
