package com.vointika.experience.infrastructure.persistence.repository;

import com.vointika.experience.infrastructure.persistence.entity.SlotPickupLocationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SlotPickupLocationJpaRepository
        extends JpaRepository<SlotPickupLocationJpaEntity, UUID> {

    List<SlotPickupLocationJpaEntity> findBySlotId(UUID slotId);

    List<SlotPickupLocationJpaEntity> findBySlotIdIn(Collection<UUID> slotIds);

    /**
     * Bulk-links a freshly created pickup to every existing slot of the operator
     * — one INSERT…SELECT. gen_random_uuid() is Postgres-core (≥13).
     *
     * <p>{@code flushAutomatically} is LOAD-BEARING (house convention): the
     * pickup save is pending in the same transaction on a different table;
     * without a flush, {@code clearAutomatically} would discard it.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO experience.pickup_location_slot
                (id, slot_id, pickup_location_id, pickup_location_name, pickup_location_time)
            SELECT gen_random_uuid(), s.id, :pickupLocationId, :pickupLocationName, :pickupLocationTime
              FROM experience.slots s
             WHERE s.tour_operator_id = :tourOperatorId
            """, nativeQuery = true)
    int bulkInsertForTourOperator(@Param("tourOperatorId") UUID tourOperatorId,
                                  @Param("pickupLocationId") UUID pickupLocationId,
                                  @Param("pickupLocationName") String pickupLocationName,
                                  @Param("pickupLocationTime") LocalTime pickupLocationTime);

    /**
     * Syncs a pickup rename / time change onto every slot row snapshotting it.
     * {@code flushAutomatically} is LOAD-BEARING — see bulkInsertForTourOperator.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SlotPickupLocationJpaEntity s set s.pickupLocationName = :name, "
            + "s.pickupLocationTime = :time where s.pickupLocationId = :pickupLocationId")
    int updateSnapshotByPickupLocationId(@Param("pickupLocationId") UUID pickupLocationId,
                                         @Param("name") String name,
                                         @Param("time") LocalTime time);

    /**
     * Removes a deleted pickup's rows from every slot (stops being offered).
     * {@code flushAutomatically} is LOAD-BEARING — see bulkInsertForTourOperator.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from SlotPickupLocationJpaEntity s where s.pickupLocationId = :pickupLocationId")
    int deleteByPickupLocationId(@Param("pickupLocationId") UUID pickupLocationId);
}
