package com.vointika.pickup.domain.repository;

import com.vointika.pickup.domain.entity.PickupLocation;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;

import java.util.Optional;
import java.util.UUID;

public interface PickupLocationRepository {

    PickupLocation save(PickupLocation pickupLocation);

    /** Tenant-scoped lookup — an id under a different operator resolves empty. */
    Optional<PickupLocation> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    /** The operator's pickup locations, cursor-paginated + filtered. */
    CursorPage<PickupLocation> list(ListQuery query);

    /** Case-insensitive per-operator name uniqueness ("Old Port" == "old port"). */
    boolean existsByTourOperatorIdAndName(UUID tourOperatorId, String name);

    /** As above, ignoring one pickup — lets it keep its own name on update. */
    boolean existsByTourOperatorIdAndNameExcluding(UUID tourOperatorId, String name, UUID excludingId);

    void deleteById(UUID id);
}
