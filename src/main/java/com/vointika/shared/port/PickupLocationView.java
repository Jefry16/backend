package com.vointika.shared.port;

import java.time.LocalTime;
import java.util.UUID;

/** A pickup location's identity fields, snapshotted onto slots at create/backfill. */
public record PickupLocationView(UUID id, String name, LocalTime time) {
}
