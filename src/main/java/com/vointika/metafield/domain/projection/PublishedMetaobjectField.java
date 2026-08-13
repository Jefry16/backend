package com.vointika.metafield.domain.projection;

import com.vointika.metafield.domain.valueobject.MetafieldType;

import java.util.UUID;

/**
 * One field of one published metaobject entry, joined flat.
 *
 * <p>The storefront resolves {@code metaobject_reference} metafields in a batch,
 * and a batch of entries crossed with their fields is naturally a row per field.
 * Grouping back into entries happens in the adapter, where the entry order is
 * already the query's (id, then field position).
 *
 * <p>The entry's own columns repeat on every row. That is the trade a single
 * query makes against one query per entry, and an operator's metaobjects are
 * counted in tens.
 */
public record PublishedMetaobjectField(UUID entryId,
                                       String entryType,
                                       String entryHandle,
                                       String entryName,
                                       String fieldKey,
                                       MetafieldType fieldType,
                                       String value) {}
