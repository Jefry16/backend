package com.vointika.experience.infrastructure.persistence.repository;

import com.vointika.experience.infrastructure.persistence.entity.ExperienceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExperienceJpaRepository extends JpaRepository<ExperienceJpaEntity, UUID> {

    /**
     * The storefront listing, in the order it renders: featured first, then
     * oldest first, tie-broken on id so two rows created in the same millisecond
     * cannot swap places between requests.
     *
     * <p><b>The method name is where the ordering lives</b>, which is why
     * {@code StorefrontExperienceQueryImplTest} pins it: the adapter above hands
     * the rows straight through, so nothing else in Java could observe a change
     * to it.
     */
    List<ExperienceJpaEntity> findByTourOperatorIdAndPublishedTrueOrderByFeaturedDescCreatedAtAscIdAsc(
            UUID tourOperatorId);

    Optional<ExperienceJpaEntity> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    boolean existsByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    boolean existsByTourOperatorIdAndHandle(UUID tourOperatorId, String handle);

    boolean existsByTourOperatorIdAndHandleAndIdNot(UUID tourOperatorId, String handle, UUID excludeExperienceId);
}
