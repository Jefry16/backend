package com.vointika.shared.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * The initiator of an audited action. {@code USER} actors carry the admin
 * user's id; {@code SYSTEM} actors must not — there is no user behind them,
 * and a fake id would corrupt the trail.
 */
public record AuditActor(AuditActorType type, UUID id) {

    public AuditActor {
        Objects.requireNonNull(type, "type must not be null");
        if ((type == AuditActorType.USER) != (id != null)) {
            throw new IllegalArgumentException(
                    "USER actors require an id; SYSTEM actors must not carry one");
        }
    }

    public static AuditActor user(UUID userId) {
        return new AuditActor(AuditActorType.USER, userId);
    }

    public static AuditActor system() {
        return new AuditActor(AuditActorType.SYSTEM, null);
    }
}
