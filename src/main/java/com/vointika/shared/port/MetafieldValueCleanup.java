package com.vointika.shared.port;

import java.util.UUID;

/**
 * Cross-context write seam: removes the metafield values a deleted resource
 * owned. Implemented by the metafield context, consumed by whichever context
 * deletes an owner — today only {@code page}, since experiences have no delete.
 *
 * <p><b>Why a port and not a foreign key.</b> {@code metafield_values.owner_id}
 * is a plain UUID with no FK, which is exactly what lets one value table serve
 * every owner type; the price is that the database cannot cascade. Nothing
 * cleaned up after it, so a deleted page left its values behind forever —
 * unreadable (every read starts from an ownership-checked owner id), unlistable
 * and undeletable.
 *
 * <p>It runs <b>inside the owner's delete transaction</b> rather than off a
 * Kafka event, so there is no window in which the owner is gone and its values
 * are not: either both go or neither does. The audit entry the caller writes
 * covers the whole operation for the same reason.
 */
public interface MetafieldValueCleanup {

    /**
     * Deletes every value owned by this resource. Owner ids are UUIDs and
     * unique across owner types, so no owner type is needed to disambiguate —
     * and passing one would make the caller import metafield's enum, which the
     * context boundary forbids.
     */
    void deleteValuesOwnedBy(UUID ownerId);
}
