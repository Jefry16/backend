package com.vointika.metafield.domain.projection;

import com.vointika.metafield.domain.valueobject.MetafieldType;

import java.time.Instant;

/**
 * One stored value joined with its definition's identity — the per-resource
 * metafields read (definitions without a value on this resource simply don't
 * appear; the admin editor overlays this onto the definitions list).
 */
public record MetafieldValueWithDefinition(
        String namespace,
        String key,
        MetafieldType type,
        String name,
        String value,
        Instant updatedAt
) {}
