package com.vointika.metafield.presentation.response;

import com.vointika.metafield.application.dto.output.MetaobjectDefinitionView;
import com.vointika.metafield.domain.valueobject.MetafieldDescription;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** {@code id} + {@code context:"metaobject-definitions"} per the house rule. */
public record MetaobjectDefinitionResponse(
        UUID id,
        String context,
        String type,
        String name,
        String description,
        List<FieldResponse> fields,
        Instant createdAt,
        Instant updatedAt) {

    /** Fields in position order. */
    public record FieldResponse(String key, String type, String name) {
    }

    public static MetaobjectDefinitionResponse from(MetaobjectDefinitionView view) {
        return new MetaobjectDefinitionResponse(
                view.definition().getId(),
                "metaobject-definitions",
                view.definition().getType().value(),
                view.definition().getName().value(),
                view.definition().getDescription().map(MetafieldDescription::value).orElse(null),
                view.fields().stream()
                        .map(f -> new FieldResponse(
                                f.getKey().value(), f.getType().code(), f.getName().value()))
                        .toList(),
                view.definition().getCreatedAt(),
                view.definition().getUpdatedAt());
    }
}
