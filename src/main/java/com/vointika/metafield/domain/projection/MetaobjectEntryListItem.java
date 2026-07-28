package com.vointika.metafield.domain.projection;

import java.time.Instant;
import java.util.UUID;

/** An entries-list row (field values live on the detail read). */
public record MetaobjectEntryListItem(
        UUID id,
        UUID definitionId,
        String handle,
        String name,
        boolean published,
        Instant createdAt) {
}
