package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.domain.repository.TourOperatorBrandRepository;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorBrandJpaEntity;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TourOperatorBrandRepositoryImpl implements TourOperatorBrandRepository {

    private final TourOperatorBrandJpaRepository jpaRepository;

    public TourOperatorBrandRepositoryImpl(TourOperatorBrandJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<UUID> findLogoMediaId(UUID tourOperatorId) {
        return jpaRepository.findById(tourOperatorId)
                .map(TourOperatorBrandJpaEntity::getLogoMediaId);
    }

    /**
     * The nulls below never reach Postgres: every column but the logo maps
     * {@code insertable/updatable = false}, so they are absent from the INSERT
     * and the UPDATE alike. Rebuilding the whole entity is what
     * {@code TourOperatorMapper} already does for phone and email, and it is
     * safe for exactly the same reason.
     */
    @Override
    public void setLogoMediaId(UUID tourOperatorId, UUID mediaId) {
        Instant now = Instant.now();
        Instant createdAt = jpaRepository.findById(tourOperatorId)
                .map(TourOperatorBrandJpaEntity::getCreatedAt)
                .orElse(now);
        jpaRepository.save(new TourOperatorBrandJpaEntity(
                tourOperatorId, null, null, mediaId, null, null, null, createdAt, now));
    }
}
