package com.vointika.shared.port;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Cross-context resolution of media ids to their storage keys, scoped to an
 * operator. The seam other contexts (experience galleries, operator logo) use
 * to turn stored media-id references back into keys — then {@code MediaUrlResolver}
 * turns keys into absolute URLs at read time.
 *
 * <p><b>Tenant-scoped:</b> an id not owned by {@code tourOperatorId} is simply
 * absent from the result — a consumer treats a miss as an invalid reference
 * (validation → 422) and never trusts a cross-tenant id. Implemented in
 * {@code media.infrastructure.query}.
 */
public interface MediaKeyBatchQuery {

    Map<UUID, String> findKeysByIds(UUID tourOperatorId, Set<UUID> mediaIds);
}
