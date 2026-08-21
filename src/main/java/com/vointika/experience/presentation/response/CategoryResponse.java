package com.vointika.experience.presentation.response;

import com.vointika.experience.domain.entity.Category;

import java.time.Instant;
import java.util.UUID;

/**
 * A category for read APIs. {@code id} + {@code context:"categories"} per the
 * house rule (never a prefixed id / {@code type}). Shared by the list rows and
 * the single-category read.
 */
public record CategoryResponse(
        UUID id,
        String context,
        String name,
        Instant createdAt) {

    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), "categories", c.getName().value(), c.getCreatedAt());
    }
}
