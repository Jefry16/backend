package com.vointika.experience.infrastructure.persistence.repository;

import com.vointika.experience.infrastructure.persistence.entity.SlotAudiencePricingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SlotAudiencePricingJpaRepository
        extends JpaRepository<SlotAudiencePricingJpaEntity, UUID> {

    List<SlotAudiencePricingJpaEntity> findBySlotId(UUID slotId);

    List<SlotAudiencePricingJpaEntity> findBySlotIdIn(Collection<UUID> slotIds);

    /**
     * Syncs the snapshotted identity fields onto every row for this audience
     * (rename / pax change).
     *
     * <p>{@code flushAutomatically} is LOAD-BEARING: the audience update saves
     * the audience first in the same transaction, and that merge is pending
     * until flush. This bulk update's table doesn't overlap the pending change,
     * so Hibernate would NOT auto-flush — and {@code clearAutomatically} would
     * then silently discard the pending audience save (slots renamed, audience
     * not). Flush-before-execute makes the clear safe.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SlotAudiencePricingJpaEntity s set s.audienceName = :name, s.paxPerUnit = :pax "
            + "where s.audienceId = :audienceId")
    int updateSnapshotByAudienceId(@Param("audienceId") UUID audienceId,
                                   @Param("name") String name,
                                   @Param("pax") int pax);
}
