package com.vointika.pickup.domain.repository;

import com.vointika.pickup.domain.entity.PickupLocation;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;

import java.util.Optional;
import java.util.UUID;

public interface PickupLocationRepository {

    PickupLocation save(PickupLocation pickupLocation);

    /**
     * The 404 for an id that resolves to nothing this operator owns.
     *
     * <p>Here rather than on a shared port — unlike audience, experience and page —
     * because nothing outside {@code pickup} reaches a pickup location. When the
     * booking context starts consuming them, this moves to the seam it goes through.
     */
    String NOT_FOUND = "Pickup location not found";

    /** Thrown by the pre-check and by the unique-index race, which must answer identically (PATTERNS §8d). */
    String NAME_TAKEN = "A pickup location with this name already exists";

    /** Tenant-scoped lookup — an id under a different operator resolves empty. */
    Optional<PickupLocation> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    /**
     * The tenant-scoped read. Every caller uses the row it loads, so there is no
     * {@code requireExists} here (contrast `page`, whose translation endpoints only
     * needed existence — PATTERNS §9).
     */
    default PickupLocation requireByIdAndTourOperatorId(UUID id, UUID tourOperatorId) {
        return findByIdAndTourOperatorId(id, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    }

    /** The operator's pickup locations, cursor-paginated + filtered. */
    CursorPage<PickupLocation> list(ListQuery query);

    /** Case-insensitive per-operator name uniqueness ("Old Port" == "old port"). */
    boolean existsByTourOperatorIdAndName(UUID tourOperatorId, String name);

    /** As above, ignoring one pickup — lets it keep its own name on update. */
    boolean existsByTourOperatorIdAndNameExcluding(UUID tourOperatorId, String name, UUID excludingId);

    void deleteById(UUID id);
}
