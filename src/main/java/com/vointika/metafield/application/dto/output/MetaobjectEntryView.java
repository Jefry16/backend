package com.vointika.metafield.application.dto.output;

import com.vointika.metafield.domain.entity.MetaobjectEntry;

import java.util.List;

/**
 * The detail read: the entry plus EVERY field of its definition in position
 * order, each with the stored value or null when unset — the editor renders
 * one input per field regardless.
 */
public record MetaobjectEntryView(
        MetaobjectEntry entry,
        List<EntryFieldValue> fields) {

    public record EntryFieldValue(String key, String type, String name, String value) {
    }
}
