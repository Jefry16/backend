package com.vointika.metafield.presentation.request;

import com.vointika.metafield.application.dto.input.CreateMetaobjectDefinitionInput.FieldSpec;

import java.util.List;

/**
 * Create a definition with its initial fields. The field nodes bind straight to
 * {@code FieldSpec} (PATTERNS §4c): the presentation copy of it was
 * field-for-field identical, so it insulated nothing while costing a copy loop.
 * The wrapper stays — the input adds the caller and the operator id.
 */
public record CreateMetaobjectDefinitionRequest(
        String type, String name, String description, List<FieldSpec> fields) {
}
