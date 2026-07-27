package com.vointika.metafield.presentation.request;

/** The editable attributes only; null/blank description clears it. */
public record UpdateMetafieldDefinitionRequest(String name, String description) {
}
