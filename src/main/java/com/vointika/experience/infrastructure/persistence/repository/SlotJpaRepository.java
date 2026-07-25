package com.vointika.experience.infrastructure.persistence.repository;

import com.vointika.experience.infrastructure.persistence.entity.SlotJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SlotJpaRepository extends JpaRepository<SlotJpaEntity, UUID> {

    Optional<SlotJpaEntity> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    /**
     * Refreshes the experience name/description snapshot on every slot of the
     * experience (an experience edit keeps existing departures in sync). Timing
     * is untouched — a slot's startAt/endAt are explicit and immutable.
     */
    @Modifying(clearAutomatically = true)
    @Query("update SlotJpaEntity s set s.experienceName = :name, s.experienceDescription = :description "
            + "where s.experienceId = :experienceId")
    int propagateExperienceSnapshot(@Param("experienceId") UUID experienceId,
                                    @Param("name") String name,
                                    @Param("description") String description);
}
