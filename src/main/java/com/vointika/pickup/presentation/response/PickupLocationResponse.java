package com.vointika.pickup.presentation.response;

import com.vointika.pickup.domain.entity.PickupLocation;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

/**
 * A pickup location for read APIs. {@code id} + {@code context:"pickup-locations"}
 * per the house rule. Shared by the list rows and the single read.
 */
public record PickupLocationResponse(
        UUID id,
        String context,
        String name,
        LocalTime time,
        Instant createdAt) {

    public static PickupLocationResponse from(PickupLocation p) {
        return new PickupLocationResponse(
                p.getId(), "pickup-locations", p.getName().value(), p.getTime().value(), p.getCreatedAt());
    }
}
