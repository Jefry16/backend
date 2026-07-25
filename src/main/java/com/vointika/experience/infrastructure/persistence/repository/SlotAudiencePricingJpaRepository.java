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

    /** Syncs the snapshotted identity fields onto every row for this audience (rename / pax change). */
    @Modifying(clearAutomatically = true)
    @Query("update SlotAudiencePricingJpaEntity s set s.audienceName = :name, s.paxPerUnit = :pax "
            + "where s.audienceId = :audienceId")
    int updateSnapshotByAudienceId(@Param("audienceId") UUID audienceId,
                                   @Param("name") String name,
                                   @Param("pax") int pax);
}
