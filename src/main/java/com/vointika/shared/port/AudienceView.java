package com.vointika.shared.port;

import java.util.UUID;

/** An audience's identity fields, snapshotted onto a slot's pricing at create. */
public record AudienceView(UUID id, String name, int paxPerUnit) {
}
