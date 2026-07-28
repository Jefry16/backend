package com.vointika.metafield.presentation.request;

import java.util.List;

public record CreateMetaobjectDefinitionRequest(
        String type, String name, String description, List<FieldRequest> fields) {

    public record FieldRequest(String key, String type, String name) {
    }
}
