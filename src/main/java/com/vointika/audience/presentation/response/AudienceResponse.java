package com.vointika.audience.presentation.response;

import com.vointika.audience.domain.entity.Audience;

import java.time.Instant;
import java.util.UUID;

/**
 * An audience for read APIs. {@code id} + {@code context:"audiences"} per the
 * house rule (never a prefixed id / {@code type}). Shared by the list rows and
 * the single-audience read.
 */
public record AudienceResponse(
        UUID id,
        String context,
        String name,
        int paxPerUnit,
        Instant createdAt) {

    public static AudienceResponse from(Audience a) {
        return new AudienceResponse(
                a.getId(), "audiences", a.getName().value(), a.getPaxPerUnit().value(), a.getCreatedAt());
    }
}
