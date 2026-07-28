package com.vointika.metafield.presentation.response;

import com.vointika.metafield.application.dto.output.MetaobjectEntryView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** {@code id} + {@code context:"metaobjects"} per the house rule. */
public record MetaobjectResponse(
        UUID id,
        String context,
        UUID definitionId,
        String handle,
        String name,
        boolean published,
        List<FieldValueResponse> fields,
        Instant createdAt,
        Instant updatedAt) {

    /** Every definition field in position order; {@code value} null when unset. */
    public record FieldValueResponse(String key, String type, String name, String value) {
    }

    public static MetaobjectResponse from(MetaobjectEntryView view) {
        return new MetaobjectResponse(
                view.entry().getId(),
                "metaobjects",
                view.entry().getDefinitionId(),
                view.entry().getHandle().value(),
                view.entry().getName().value(),
                view.entry().isPublished(),
                view.fields().stream()
                        .map(f -> new FieldValueResponse(f.key(), f.type(), f.name(), f.value()))
                        .toList(),
                view.entry().getCreatedAt(),
                view.entry().getUpdatedAt());
    }
}
