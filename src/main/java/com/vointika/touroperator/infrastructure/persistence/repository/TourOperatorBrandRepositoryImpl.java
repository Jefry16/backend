package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.domain.entity.Brand;
import com.vointika.touroperator.domain.repository.TourOperatorBrandRepository;
import com.vointika.touroperator.infrastructure.persistence.mapper.TourOperatorBrandMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class TourOperatorBrandRepositoryImpl implements TourOperatorBrandRepository {

    private final TourOperatorBrandJpaRepository brandRepository;
    private final TourOperatorBrandColorJpaRepository colorRepository;
    private final TourOperatorBrandSocialLinkJpaRepository socialLinkRepository;

    public TourOperatorBrandRepositoryImpl(
            TourOperatorBrandJpaRepository brandRepository,
            TourOperatorBrandColorJpaRepository colorRepository,
            TourOperatorBrandSocialLinkJpaRepository socialLinkRepository) {
        this.brandRepository = brandRepository;
        this.colorRepository = colorRepository;
        this.socialLinkRepository = socialLinkRepository;
    }

    @Override
    public Optional<Brand> findByTourOperatorId(UUID tourOperatorId) {
        return brandRepository.findById(tourOperatorId)
                .map(jpa -> TourOperatorBrandMapper.toDomain(
                        jpa,
                        colorRepository.findByTourOperatorIdOrderByPositionAsc(tourOperatorId),
                        socialLinkRepository.findByTourOperatorIdOrderByPlatformAsc(tourOperatorId)));
    }

    /**
     * <b>Delete-then-insert, not a diff.</b> Both collections are keyed on values
     * the payload can reorder — a colour's key is {@code (operator, role,
     * position)} — so a diff would have to match rows that moved, and a palette
     * whose entries swap places is exactly the realistic edit. Replacing the set
     * makes the stored order the payload's order by construction.
     *
     * <p>Runs inside the use case's transaction, so a failed insert rolls the
     * delete back with it and the operator never sees a half-cleared palette.
     */
    @Override
    public Brand save(Brand brand) {
        UUID operatorId = brand.tourOperatorId();
        brandRepository.save(TourOperatorBrandMapper.toJpa(brand));

        colorRepository.deleteByTourOperatorId(operatorId);
        socialLinkRepository.deleteByTourOperatorId(operatorId);
        // Flush the deletes before the inserts: without it Hibernate may order the
        // INSERTs first and collide with the rows still present on the same keys.
        colorRepository.flush();
        socialLinkRepository.flush();

        colorRepository.saveAll(brand.colors().stream()
                .map(c -> TourOperatorBrandMapper.toJpa(operatorId, c)).toList());
        socialLinkRepository.saveAll(brand.socialLinks().stream()
                .map(l -> TourOperatorBrandMapper.toJpa(operatorId, l)).toList());
        return brand;
    }
}
