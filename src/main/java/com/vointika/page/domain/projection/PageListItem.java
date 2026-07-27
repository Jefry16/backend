package com.vointika.page.domain.projection;

import com.vointika.page.domain.enums.PageStatus;

import java.time.Instant;
import java.util.UUID;

/** A pages-list row — deliberately WITHOUT {@code body} (heavy HTML stays on the detail read). */
public record PageListItem(
        UUID id,
        String title,
        String handle,
        PageStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
